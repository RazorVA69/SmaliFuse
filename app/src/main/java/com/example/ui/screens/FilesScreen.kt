package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmaliSource
import com.example.ui.components.SmaliFileCard
import com.example.ui.components.StatusSquiggleCard
import com.example.ui.components.StoragePermissionCard
import com.example.ui.viewmodel.SmaliMergerUiState
import com.example.ui.viewmodel.SmaliMergerViewModel

@Composable
fun FilesScreen(
    viewModel: SmaliMergerViewModel,
    uiState: SmaliMergerUiState,
    onRequestStoragePermission: () -> Unit,
    onNavigateToOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Launchers for picking files, folder tree, and zip archives
    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.loadDirectFiles(uris, context)
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            viewModel.loadFolder(uri, context)
        }
    }

    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.loadZipArchive(uri, context)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Signature Morphe / ReVanced Squiggle Loading Card from screenshot
            val badgeCount = if (uiState.sources.isNotEmpty()) {
                "${uiState.selectedCount} / ${uiState.sources.size}"
            } else ""

            StatusSquiggleCard(
                isProcessing = uiState.isProcessing,
                progress = uiState.processingProgress,
                statusTitle = uiState.processingTitle,
                statusSubtitle = uiState.processingSubtitle,
                badgeText = "Smali Merger",
                totalCountText = badgeCount
            )
        }

        // 2. Storage Permission Card (if not yet granted or user has prompt)
        if (uiState.showPermissionNotice && !uiState.isStoragePermissionGranted) {
            item {
                StoragePermissionCard(
                    isPermissionGranted = uiState.isStoragePermissionGranted,
                    onRequestPermission = onRequestStoragePermission,
                    onDismissOrContinue = { viewModel.dismissPermissionNotice() }
                )
            }
        }

        // 3. File Input Action Pills (Morphe/ReVanced screenshot bottom buttons aesthetic)
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Smali Sources",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pick Multiple Files
                    ActionButtonPill(
                        label = "Choose Files",
                        icon = Icons.Default.Description,
                        containerColor = Color(0xFF00897B),
                        contentColor = Color.White,
                        testTag = "btn_pick_files",
                        onClick = {
                            pickFilesLauncher.launch(arrayOf("*/*", "text/*"))
                        }
                    )

                    // Pick Folder Tree
                    ActionButtonPill(
                        label = "Choose Folder",
                        icon = Icons.Default.Folder,
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White,
                        testTag = "btn_pick_folder",
                        onClick = {
                            pickFolderLauncher.launch(null)
                        }
                    )

                    // Pick ZIP Archive
                    ActionButtonPill(
                        label = "Choose ZIP",
                        icon = Icons.Default.FolderZip,
                        containerColor = Color(0xFFE65100),
                        contentColor = Color.White,
                        testTag = "btn_pick_zip",
                        onClick = {
                            pickZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        }
                    )

                    // Restore Demo
                    ActionButtonPill(
                        label = "Load Demo",
                        icon = Icons.Default.RestartAlt,
                        containerColor = Color(0xFF5E35B1),
                        contentColor = Color.White,
                        testTag = "btn_load_demo",
                        onClick = {
                            viewModel.restoreDemoSamples()
                            Toast.makeText(context, "Loaded demo smali files", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // 4. Search and Filter Chips
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text("Search classes, paths, or code...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00897B),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00897B),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }

        // 5. Filter categories row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "All" to uiState.sources.size,
                    "Smali" to uiState.sources.count { it.isSmaliExtension },
                    "No-Ext" to uiState.sources.count { !it.isSmaliExtension },
                    "ZIP" to uiState.sources.count { it.origin == com.example.data.model.SourceOrigin.ZIP_ARCHIVE }
                ).forEach { (filter, count) ->
                    val isSelected = uiState.activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setActiveFilter(filter) },
                        label = { Text("$filter ($count)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00897B),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }
        }

        // 6. Multi-select toggle bar & Clear
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val allSelected = uiState.sources.isNotEmpty() && uiState.sources.all { it.isSelected }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { viewModel.selectAll(!allSelected) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00897B)),
                            modifier = Modifier.testTag("select_all_checkbox")
                        )
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All (${uiState.selectedCount})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.sources.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearAllSources() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("clear_all_btn")
                            ) {
                                Text("Clear List", fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onNavigateToOutput,
                            enabled = uiState.selectedCount > 0,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00897B),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("go_to_merged_btn")
                        ) {
                            Text("Merge (${uiState.selectedCount})", fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 7. Empty state or List of Smali Files
        if (uiState.filteredSources.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No files match '${uiState.searchQuery}'" else "No Smali files loaded yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Choose Files', 'Choose Folder', 'Choose ZIP', or 'Load Demo' above to begin merging.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(uiState.filteredSources, key = { it.id }) { source ->
                SmaliFileCard(
                    source = source,
                    onToggleSelect = { viewModel.toggleSourceSelection(source.id) },
                    onPreview = { viewModel.setPreviewingSource(source) },
                    onRemove = { viewModel.removeSource(source.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ActionButtonPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
