package com.example.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.HeaderFormat
import com.example.data.model.SmaliSource
import com.example.data.model.SourceOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object SmaliParser {

    private val SMALI_DIRECTIVES = listOf(
        ".class", ".super", ".source", ".implements", ".field",
        ".method", ".end method", "invoke-", "return-", "const-",
        "move-result", "sget-", "sput-", "iget-", "iput-", "new-instance"
    )

    fun hasSmaliDirectives(content: String): Boolean {
        val sample = if (content.length > 2000) content.substring(0, 2000) else content
        return SMALI_DIRECTIVES.any { sample.contains(it) }
    }

    private fun isLikelyText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        var nullCount = 0
        val checkLen = minOf(bytes.size, 1024)
        for (i in 0 until checkLen) {
            val b = bytes[i].toInt()
            if (b == 0) nullCount++
        }
        return (nullCount.toDouble() / checkLen) < 0.01
    }

    data class DirectFilesResult(
        val sources: List<SmaliSource>,
        val skippedZipCount: Int = 0
    )

    suspend fun parseDirectFiles(
        uris: List<Uri>,
        context: Context,
        includeExtensionless: Boolean,
        ignoreZips: Boolean = false,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): DirectFilesResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<SmaliSource>()
        var skippedZips = 0
        val total = uris.size

        uris.forEachIndexed { index, uri ->
            val fileName = queryFileName(context, uri) ?: "file_$index"
            onProgress(index + 1, total, fileName)

            try {
                // If it's a zip archive picked through direct files
                if (fileName.endsWith(".zip", ignoreCase = true)) {
                    if (ignoreZips) {
                        skippedZips++
                    } else {
                        val zipItems = parseZipStream(
                            context.contentResolver.openInputStream(uri),
                            fileName,
                            includeExtensionless
                        )
                        results.addAll(zipItems)
                    }
                } else {
                    val isSmali = fileName.endsWith(".smali", ignoreCase = true)
                    val hasNoExt = !fileName.contains(".")

                    if (isSmali || (includeExtensionless && hasNoExt)) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (isLikelyText(bytes)) {
                                val content = String(bytes, StandardCharsets.UTF_8)
                                val hasDirectives = hasSmaliDirectives(content)
                                val lineCount = content.lines().size
                                results.add(
                                    SmaliSource(
                                        fileName = fileName,
                                        relativePath = fileName,
                                        content = content,
                                        sizeBytes = bytes.size.toLong(),
                                        lineCount = lineCount,
                                        isSmaliExtension = isSmali,
                                        hasSmaliDirectives = hasDirectives,
                                        origin = SourceOrigin.DIRECT_FILE
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        DirectFilesResult(results, skippedZips)
    }

    suspend fun parseFolder(
        treeUri: Uri,
        context: Context,
        includeExtensionless: Boolean,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): List<SmaliSource> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val results = mutableListOf<SmaliSource>()
        val allFiles = mutableListOf<Pair<DocumentFile, String>>()

        fun scanDoc(doc: DocumentFile, currentPath: String) {
            if (doc.isDirectory) {
                doc.listFiles().forEach { child ->
                    val nextPath = if (currentPath.isEmpty()) child.name ?: "" else "$currentPath/${child.name ?: ""}"
                    scanDoc(child, nextPath)
                }
            } else if (doc.isFile) {
                allFiles.add(doc to currentPath)
            }
        }

        scanDoc(rootDoc, "")
        val total = allFiles.size

        allFiles.forEachIndexed { index, (fileDoc, relPath) ->
            val name = fileDoc.name ?: "unknown"
            onProgress(index + 1, total, name)

            if (name.endsWith(".zip", ignoreCase = true)) {
                try {
                    context.contentResolver.openInputStream(fileDoc.uri)?.use { stream ->
                        val zipItems = parseZipStream(stream, name, includeExtensionless)
                        results.addAll(zipItems)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val isSmali = name.endsWith(".smali", ignoreCase = true)
                val hasNoExt = !name.contains(".")

                if (isSmali || (includeExtensionless && hasNoExt)) {
                    try {
                        context.contentResolver.openInputStream(fileDoc.uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (isLikelyText(bytes)) {
                                val content = String(bytes, StandardCharsets.UTF_8)
                                val hasDirectives = hasSmaliDirectives(content)
                                results.add(
                                    SmaliSource(
                                        fileName = name,
                                        relativePath = relPath,
                                        content = content,
                                        sizeBytes = bytes.size.toLong(),
                                        lineCount = content.lines().size,
                                        isSmaliExtension = isSmali,
                                        hasSmaliDirectives = hasDirectives,
                                        origin = SourceOrigin.DIRECTORY_SCAN,
                                        originContainer = rootDoc.name
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        results
    }

    suspend fun parseZip(
        zipUri: Uri,
        context: Context,
        includeExtensionless: Boolean,
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): List<SmaliSource> = withContext(Dispatchers.IO) {
        val fileName = queryFileName(context, zipUri) ?: "archive.zip"
        context.contentResolver.openInputStream(zipUri)?.use { stream ->
            parseZipStream(stream, fileName, includeExtensionless, onProgress)
        } ?: emptyList()
    }

    private fun parseZipStream(
        stream: InputStream?,
        zipName: String,
        includeExtensionless: Boolean,
        onProgress: ((current: Int, total: Int, name: String) -> Unit)? = null
    ): List<SmaliSource> {
        if (stream == null) return emptyList()
        val items = mutableListOf<SmaliSource>()
        var count = 0

        ZipInputStream(stream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    val simpleName = entryName.substringAfterLast('/')
                    val isSmali = entryName.endsWith(".smali", ignoreCase = true)
                    val hasNoExt = !simpleName.contains(".")

                    if (isSmali || (includeExtensionless && hasNoExt)) {
                        count++
                        onProgress?.invoke(count, 0, simpleName)
                        val bytes = readEntryBytes(zis)
                        if (isLikelyText(bytes)) {
                            val content = String(bytes, StandardCharsets.UTF_8)
                            val hasDirectives = hasSmaliDirectives(content)
                            items.add(
                                SmaliSource(
                                    fileName = simpleName,
                                    relativePath = entryName,
                                    content = content,
                                    sizeBytes = bytes.size.toLong(),
                                    lineCount = content.lines().size,
                                    isSmaliExtension = isSmali,
                                    hasSmaliDirectives = hasDirectives,
                                    origin = SourceOrigin.ZIP_ARCHIVE,
                                    originContainer = zipName
                                )
                            )
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return items
    }

    private fun readEntryBytes(zis: ZipInputStream): ByteArray {
        val buffer = ByteArray(8192)
        val baos = ByteArrayOutputStream()
        var len: Int
        while (zis.read(buffer).also { len = it } != -1) {
            baos.write(buffer, 0, len)
        }
        return baos.toByteArray()
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    fun generateMergedText(
        sources: List<SmaliSource>,
        format: HeaderFormat
    ): String {
        val sb = StringBuilder()
        val selectedSources = sources.filter { it.isSelected }

        selectedSources.forEachIndexed { index, source ->
            when (format) {
                HeaderFormat.PATH_THEN_NAME -> {
                    // Prompt's exact specification:
                    // ===== Path kotlin/sequences/SequenceScope.smali =====
                    // File name
                    // [contents]
                    sb.append("===== Path ${source.relativePath} =====\n")
                    sb.append("${source.fileName}\n")
                    sb.append(source.content)
                }
                HeaderFormat.FILE_ONLY -> {
                    // Prompt's alternative specification:
                    // ===== File: SomeOtherClass.smali =====
                    // [contents]
                    sb.append("===== File: ${source.fileName} =====\n")
                    sb.append(source.content)
                }
                HeaderFormat.PATH_DETAILED -> {
                    sb.append("================================================================================\n")
                    sb.append("===== Path: ${source.relativePath} =====\n")
                    sb.append("File: ${source.fileName}\n")
                    sb.append("Origin: ${source.origin.name}${if (source.originContainer != null) " (${source.originContainer})" else ""}\n")
                    sb.append("================================================================================\n")
                    sb.append(source.content)
                }
            }
            if (index < selectedSources.size - 1) {
                sb.append("\n\n")
            }
        }
        return sb.toString()
    }

    fun getDemoSamples(): List<SmaliSource> {
        val sample1 = """
            .class public abstract Lkotlin/sequences/SequenceScope;
            .super Ljava/lang/Object;
            .source "SequenceBuilder.kt"

            # direct methods
            .method public constructor <init>()V
                .registers 1

                .line 1
                invoke-direct {p0}, Ljava/lang/Object;-><init>()V

                return-void
            .end method

            # virtual methods
            .method public abstract yield(Ljava/lang/Object;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
                .annotation system Ldalvik/annotation/Signature;
                    value = {
                        "(TT;",
                        "Lkotlin/coroutines/Continuation<",
                        "-",
                        "Lkotlin/Unit;",
                        ">;)",
                        "Ljava/lang/Object;"
                    }
                .end annotation
            .end method
        """.trimIndent()

        val sample2 = """
            .class public final Lcom/utility/Adsutility;
            .super Ljava/lang/Object;
            .source "Adsutility.java"

            # static fields
            .field private static isAdEnabled:Z = false

            # direct methods
            .method public constructor <init>()V
                .registers 1
                invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                return-void
            .end method

            .method public static checkAndShowBanner(Landroid/content/Context;)V
                .registers 2
                sget-boolean v0, Lcom/utility/Adsutility;->isAdEnabled:Z
                if-eqz v0, :cond_show
                return-void

                :cond_show
                const-string v0, "AdsUtility"
                const-string v1, "Suppressing banner call"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                return-void
            .end method
        """.trimIndent()

        val sample3 = """
            .class public Lcom/example/MainActivity;
            .super Landroidx/activity/ComponentActivity;
            .source "MainActivity.kt"

            # direct methods
            .method public constructor <init>()V
                .registers 1
                invoke-direct {p0}, Landroidx/activity/ComponentActivity;-><init>()V
                return-void
            .end method

            .method protected onCreate(Landroid/os/Bundle;)V
                .registers 3
                invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V
                return-void
            .end method
        """.trimIndent()

        val sample4 = """
            .class public final Lcom/network/SecureNetworkClient;
            .super Ljava/lang/Object;
            .source "SecureNetworkClient.kt"

            # direct methods
            .method public static fetchApiData(Ljava/lang/String;)Ljava/lang/String;
                .registers 2
                const-string v0, "Status: OK 200"
                return-object v0
            .end method
        """.trimIndent()

        return listOf(
            SmaliSource(
                fileName = "SequenceScope.smali",
                relativePath = "kotlin/sequences/SequenceScope.smali",
                content = sample1,
                sizeBytes = sample1.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                lineCount = sample1.lines().size,
                isSmaliExtension = true,
                hasSmaliDirectives = true,
                origin = SourceOrigin.SAMPLE_DEMO,
                originContainer = "kotlin-stdlib.zip"
            ),
            SmaliSource(
                fileName = "Adsutility",
                relativePath = "com/utility/Adsutility",
                content = sample2,
                sizeBytes = sample2.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                lineCount = sample2.lines().size,
                isSmaliExtension = false, // Extensionless like user prompt requested!
                hasSmaliDirectives = true,
                origin = SourceOrigin.SAMPLE_DEMO,
                originContainer = "dex_extract"
            ),
            SmaliSource(
                fileName = "MainActivity.smali",
                relativePath = "com/example/MainActivity.smali",
                content = sample3,
                sizeBytes = sample3.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                lineCount = sample3.lines().size,
                isSmaliExtension = true,
                hasSmaliDirectives = true,
                origin = SourceOrigin.SAMPLE_DEMO
            ),
            SmaliSource(
                fileName = "SecureNetworkClient.smali",
                relativePath = "com/network/SecureNetworkClient.smali",
                content = sample4,
                sizeBytes = sample4.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                lineCount = sample4.lines().size,
                isSmaliExtension = true,
                hasSmaliDirectives = true,
                origin = SourceOrigin.SAMPLE_DEMO,
                originContainer = "security.zip"
            )
        )
    }
}
