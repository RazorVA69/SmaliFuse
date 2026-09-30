package com.example.data.model

import java.util.UUID

enum class SourceOrigin {
    DIRECT_FILE,
    DIRECTORY_SCAN,
    ZIP_ARCHIVE,
    SAMPLE_DEMO
}

data class SmaliSource(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val relativePath: String,
    val content: String,
    val sizeBytes: Long,
    val lineCount: Int,
    val isSmaliExtension: Boolean,
    val hasSmaliDirectives: Boolean,
    val origin: SourceOrigin,
    val originContainer: String? = null,
    val isSelected: Boolean = true
) {
    val displayBadge: String
        get() = when {
            origin == SourceOrigin.SAMPLE_DEMO -> "Demo"
            origin == SourceOrigin.ZIP_ARCHIVE -> "ZIP"
            !isSmaliExtension -> "No-Ext"
            else -> "Smali"
        }
}

enum class HeaderFormat(val displayName: String, val description: String) {
    PATH_THEN_NAME(
        "Path & Filename",
        "===== Path <path> =====\n<filename>\n[contents]"
    ),
    FILE_ONLY(
        "File Header",
        "===== File: <filename> =====\n[contents]"
    ),
    PATH_DETAILED(
        "Detailed Path Banner",
        "========================================\n===== Path: <path> =====\nFile: <filename>\n========================================\n[contents]"
    )
}
