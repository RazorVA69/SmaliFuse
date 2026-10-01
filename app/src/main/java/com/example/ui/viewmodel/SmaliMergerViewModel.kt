package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MergeRecord
import com.example.data.model.HeaderFormat
import com.example.data.model.SmaliSource
import com.example.domain.SmaliParser
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppFontTheme
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmaliMergerUiState(
    val sources: List<SmaliSource> = emptyList(),
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val processingTitle: String = "",
    val processingSubtitle: String = "",
    val searchQuery: String = "",
    val activeFilter: String = "All",
    val headerFormat: HeaderFormat = HeaderFormat.PATH_THEN_NAME,
    val includeExtensionless: Boolean = true,
    val hideZipsInFilesPicker: Boolean = true,
    val fontTheme: AppFontTheme = AppFontTheme.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AppAccentColor = AppAccentColor.TEAL,
    val mergedOutput: String = "",
    val mergedFileCount: Int = 0,
    val mergedSizeBytes: Long = 0,
    val previewingSource: SmaliSource? = null,
    val isStoragePermissionGranted: Boolean = false,
    val showPermissionNotice: Boolean = true,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val historyList: List<MergeRecord> = emptyList()
) {
    val filteredSources: List<SmaliSource>
        get() {
            var list = sources
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.fileName.lowercase().contains(q) ||
                            it.relativePath.lowercase().contains(q) ||
                            it.content.lowercase().contains(q)
                }
            }
            return when (activeFilter) {
                "Smali" -> list.filter { it.isSmaliExtension }
                "No-Ext" -> list.filter { !it.isSmaliExtension }
                "ZIP" -> list.filter { it.origin == com.example.data.model.SourceOrigin.ZIP_ARCHIVE }
                else -> list
            }
        }

    val selectedCount: Int
        get() = sources.count { it.isSelected }

    val totalBytes: Long
        get() = sources.filter { it.isSelected }.sumOf { it.sizeBytes }
}

class SmaliMergerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val mergeDao = db.mergeDao()

    private val _uiState = MutableStateFlow(SmaliMergerUiState())
    val uiState: StateFlow<SmaliMergerUiState> = _uiState.asStateFlow()

    init {
        // Observe merge records history
        viewModelScope.launch {
            mergeDao.getAllRecords().collect { records ->
                _uiState.update { it.copy(historyList = records) }
            }
        }
    }

    private fun loadInitialDemo() {
        val samples = SmaliParser.getDemoSamples()
        _uiState.update {
            it.copy(
                sources = samples,
                processingTitle = "Smali Merger Ready",
                processingSubtitle = "${samples.size} sample files loaded (including extensionless 'Adsutility')",
                processingProgress = 1.0f
            )
        }
        performMerge()
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update {
            it.copy(
                isStoragePermissionGranted = granted,
                showPermissionNotice = !granted
            )
        }
    }

    fun dismissPermissionNotice() {
        _uiState.update { it.copy(showPermissionNotice = false) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setActiveFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setHeaderFormat(format: HeaderFormat) {
        _uiState.update { it.copy(headerFormat = format) }
        performMerge()
    }

    fun setIncludeExtensionless(include: Boolean) {
        _uiState.update { it.copy(includeExtensionless = include) }
    }

    fun setHideZipsInFilesPicker(hide: Boolean) {
        _uiState.update { it.copy(hideZipsInFilesPicker = hide) }
    }

    fun setFontTheme(theme: AppFontTheme) {
        _uiState.update { it.copy(fontTheme = theme) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAccentColor(accent: AppAccentColor) {
        _uiState.update { it.copy(accentColor = accent) }
    }

    fun setPreviewingSource(source: SmaliSource?) {
        _uiState.update { it.copy(previewingSource = source) }
    }

    fun toggleSourceSelection(id: String) {
        _uiState.update { state ->
            val updated = state.sources.map {
                if (it.id == id) it.copy(isSelected = !it.isSelected) else it
            }
            state.copy(sources = updated)
        }
        performMerge()
    }

    fun selectAll(select: Boolean) {
        _uiState.update { state ->
            val updated = state.sources.map { it.copy(isSelected = select) }
            state.copy(sources = updated)
        }
        performMerge()
    }

    fun removeSource(id: String) {
        _uiState.update { state ->
            val updated = state.sources.filterNot { it.id == id }
            state.copy(sources = updated)
        }
        performMerge()
    }

    fun clearAllSources() {
        _uiState.update {
            it.copy(
                sources = emptyList(),
                mergedOutput = "",
                processingTitle = "Ready",
                processingSubtitle = "All files cleared. Pick files to begin.",
                processingProgress = 0f
            )
        }
    }

    fun loadDirectFiles(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val hideZips = _uiState.value.hideZipsInFilesPicker
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0f,
                    processingTitle = "Reading Smali files...",
                    processingSubtitle = "Parsing ${uris.size} items"
                )
            }
            try {
                var lastUpdate = 0L
                val result = SmaliParser.parseDirectFiles(
                    uris = uris,
                    context = context,
                    includeExtensionless = _uiState.value.includeExtensionless,
                    ignoreZips = hideZips,
                    onProgress = { current, total, name ->
                        val now = System.currentTimeMillis()
                        if (current == 1 || current == total || current % 15 == 0 || now - lastUpdate > 80) {
                            lastUpdate = now
                            _uiState.update {
                                it.copy(
                                    processingProgress = if (total > 0) current.toFloat() / total else 0.5f,
                                    processingSubtitle = "Reading: $name ($current / $total)"
                                )
                            }
                        }
                    }
                )
                val statusMsg = if (result.skippedZipCount > 0) {
                    "Loaded ${result.sources.size} file(s). Skipped ${result.skippedZipCount} .zip archive(s) (use ZIP button to extract)."
                } else {
                    "Loaded ${result.sources.size} Smali file(s)"
                }
                withContext(Dispatchers.Main) {
                    appendSources(result.sources, statusMsg)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Error loading files: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadFolder(treeUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0f,
                    processingTitle = "Scanning Folder...",
                    processingSubtitle = "Searching for .smali and extensionless files..."
                )
            }
            try {
                var lastUpdate = 0L
                val parsed = SmaliParser.parseFolder(
                    treeUri = treeUri,
                    context = context,
                    includeExtensionless = _uiState.value.includeExtensionless,
                    onProgress = { current, total, name ->
                        val now = System.currentTimeMillis()
                        if (current == 1 || current == total || current % 15 == 0 || now - lastUpdate > 80) {
                            lastUpdate = now
                            _uiState.update {
                                it.copy(
                                    processingProgress = if (total > 0) current.toFloat() / total else 0.5f,
                                    processingSubtitle = "Found: $name ($current/$total)"
                                )
                            }
                        }
                    }
                )
                withContext(Dispatchers.Main) {
                    appendSources(parsed, "Scanned folder: added ${parsed.size} Smali file(s)")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Error scanning folder: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadZipArchive(zipUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0f,
                    processingTitle = "Extracting ZIP Archive...",
                    processingSubtitle = "Decompressing and inspecting entries..."
                )
            }
            try {
                var lastUpdate = 0L
                val parsed = SmaliParser.parseZip(
                    zipUri = zipUri,
                    context = context,
                    includeExtensionless = _uiState.value.includeExtensionless,
                    onProgress = { count, _, name ->
                        val now = System.currentTimeMillis()
                        if (count == 1 || count % 15 == 0 || now - lastUpdate > 80) {
                            lastUpdate = now
                            _uiState.update {
                                it.copy(
                                    processingSubtitle = "Extracted #$count: $name"
                                )
                            }
                        }
                    }
                )
                withContext(Dispatchers.Main) {
                    appendSources(parsed, "Extracted ${parsed.size} Smali file(s) from ZIP")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Error extracting ZIP: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun restoreDemoSamples() {
        val samples = SmaliParser.getDemoSamples()
        appendSources(samples, "Sample demo Smali files restored")
    }

    private fun appendSources(newSources: List<SmaliSource>, message: String) {
        _uiState.update { state ->
            // Avoid duplicate paths
            val existingPaths = state.sources.map { it.relativePath }.toSet()
            val filteredNew = newSources.filterNot { it.relativePath in existingPaths }
            val combined = state.sources + filteredNew
            state.copy(
                sources = combined,
                isProcessing = false,
                processingProgress = 1f,
                processingTitle = "Files Ready (${combined.size})",
                processingSubtitle = message,
                statusMessage = message
            )
        }
        performMerge()
    }

    fun performMerge() {
        val currentSources = _uiState.value.sources.filter { it.isSelected }
        if (currentSources.isEmpty()) {
            _uiState.update {
                it.copy(
                    mergedOutput = "",
                    mergedFileCount = 0,
                    mergedSizeBytes = 0
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val text = SmaliParser.generateMergedText(
                sources = currentSources,
                format = _uiState.value.headerFormat
            )
            val bytes = text.toByteArray().size.toLong()
            _uiState.update {
                it.copy(
                    mergedOutput = text,
                    mergedFileCount = currentSources.size,
                    mergedSizeBytes = bytes
                )
            }
        }
    }

    fun saveMergedOutputToUri(uri: Uri, context: Context) {
        val text = _uiState.value.mergedOutput
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(text)
                    }
                }

                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                val record = MergeRecord(
                    timestamp = timestamp,
                    fileName = "merged_smali_${dateFormat.format(Date(timestamp))}.txt",
                    totalFiles = _uiState.value.mergedFileCount,
                    totalSizeBytes = _uiState.value.mergedSizeBytes,
                    previewText = text.take(600),
                    fullText = text
                )
                mergeDao.insertRecord(record)

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Merged output saved successfully to storage!",
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to save file: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun deleteHistoryRecord(record: MergeRecord) {
        viewModelScope.launch {
            mergeDao.deleteRecord(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            mergeDao.clearAll()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
