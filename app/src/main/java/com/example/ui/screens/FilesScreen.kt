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
        // Show Snake Squiggle Progress ONLY when actively processing / reading files
        if (uiState.isProcessing) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                StatusSquiggleCard(
                    isProcessing = true,
                    progress = uiState.processingProgress,
                    statusTitle = uiState.processingTitle.ifBlank { "Processing Smali files..." },
                    statusSubtitle = uiState.processingSubtitle,
                    badgeText = "Working",
                    totalCountText = ""
                )
            }
        }

        // Storage Permission Card (if not yet granted or user has prompt)
        if (uiState.showPermissionNotice && !uiState.isStoragePermissionGranted) {
            item {
                StoragePermissionCard(
                    isPermissionGranted = uiState.isStoragePermissionGranted,
                    onRequestPermission = onRequestStoragePermission,
                    onDismissOrContinue = { viewModel.dismissPermissionNotice() }
                )
            }
        }

        // File Input Action Pills
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
                }
            }
        }

        // Multi-select toggle bar & Clear (shown when files are present)
        if (uiState.sources.isNotEmpty()) {
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
                        val allSelected = uiState.sources.all { it.isSelected }
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
                            OutlinedButton(
                                onClick = { viewModel.clearAllSources() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("clear_all_btn")
                            ) {
                                Text("Clear List", fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                            Spacer(modifier = Modifier.width(8.dp))

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
                            text = "Tap 'Choose Files', 'Choose Folder', or 'Choose ZIP' above to add Smali files to merge.",
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
