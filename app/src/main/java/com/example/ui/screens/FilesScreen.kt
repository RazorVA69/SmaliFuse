package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.SmaliFileCard
import com.example.ui.components.SnakeSquiggleProgressBar
import com.example.ui.components.StatusSquiggleCard
import com.example.ui.components.StoragePermissionCard
import com.example.ui.theme.PastelAmberBg
import com.example.ui.theme.PastelAmberText
import com.example.ui.theme.PastelBlueBg
import com.example.ui.theme.PastelBlueText
import com.example.ui.theme.PastelRoseBg
import com.example.ui.theme.PastelTealBg
import com.example.ui.theme.PastelTealText
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StrideTeal
import com.example.ui.theme.StrideTealAccent

@Composable
fun FilesScreen(
    viewModel: com.example.ui.viewmodel.SmaliMergerViewModel,
    uiState: com.example.ui.viewmodel.SmaliMergerUiState,
    onRequestStoragePermission: () -> Unit,
    onNavigateToOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                // Ignore
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Show snake progress card when actively reading or parsing
        if (uiState.isProcessing) {
            item {
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

        // Storage Permission Card
        if (uiState.showPermissionNotice && !uiState.isStoragePermissionGranted) {
            item {
                StoragePermissionCard(
                    isPermissionGranted = uiState.isStoragePermissionGranted,
                    onRequestPermission = onRequestStoragePermission,
                    onDismissOrContinue = { viewModel.dismissPermissionNotice() }
                )
            }
        }

        // Hero 3-Tile Import Grid (Stride Bento style)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImportTile(
                    title = "Files",
                    subtitle = if (uiState.hideZipsInFilesPicker) "Smali & text" else "Pick files",
                    icon = Icons.Default.PostAdd,
                    tintBg = PastelTealBg,
                    tintColor = PastelTealText,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_pick_files",
                    onClick = {
                        if (uiState.hideZipsInFilesPicker) {
                            // Only text/smali MIME types - hides and disables .zip files!
                            pickFilesLauncher.launch(arrayOf("text/*", "text/plain"))
                        } else {
                            pickFilesLauncher.launch(arrayOf("*/*", "text/*"))
                        }
                    }
                )

                ImportTile(
                    title = "Folder",
                    subtitle = "Scan tree",
                    icon = Icons.Default.DriveFolderUpload,
                    tintBg = PastelBlueBg,
                    tintColor = PastelBlueText,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_pick_folder",
                    onClick = { pickFolderLauncher.launch(null) }
                )

                ImportTile(
                    title = "ZIP",
                    subtitle = "Extract archive",
                    icon = Icons.Default.FolderZip,
                    tintBg = PastelAmberBg,
                    tintColor = PastelAmberText,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_pick_zip",
                    onClick = {
                        pickZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    }
                )
            }
        }

        // Summary Metric Card
        if (uiState.sources.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricItem(
                                label = "Discovered",
                                value = "${uiState.sources.size}",
                                dotColor = PastelTealText
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFFF1F5F9))
                            )
                            MetricItem(
                                label = "Selected",
                                value = "${uiState.selectedCount}",
                                dotColor = PastelBlueText
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFFF1F5F9))
                            )
                            MetricItem(
                                label = "Total Size",
                                value = formatBytes(uiState.totalBytes),
                                dotColor = PastelAmberText
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Expressive Stride squiggle wave accent
                        SnakeSquiggleProgressBar(
                            progress = if (uiState.sources.isNotEmpty()) {
                                uiState.selectedCount.toFloat() / uiState.sources.size
                            } else 0f,
                            strokeWidth = 3.dp,
                            amplitude = 3.dp,
                            wavelength = 20.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    StrideTeal,
                                    Color(0xFF0284C7),
                                    StrideTealAccent
                                )
                            ),
                            trackColor = Color(0xFFF1F5F9)
                        )
                    }
                }
            }
        }

        // Multi-select & Quick Action Row
        if (uiState.sources.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val allSelected = uiState.sources.all { it.isSelected }
                        val primaryColor = MaterialTheme.colorScheme.primary

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { viewModel.selectAll(!allSelected) },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                modifier = Modifier.testTag("select_all_checkbox")
                            )
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All (${uiState.selectedCount})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Slate700
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Modern Clear All Pill Button with DeleteSweep icon
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PastelRoseBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.clearAllSources() }
                                    .testTag("clear_all_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All Files",
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFE11D48)
                                    )
                                }
                            }

                            // Modern Merge Button with CallMerge icon
                            Button(
                                onClick = onNavigateToOutput,
                                enabled = uiState.selectedCount > 0,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.testTag("go_to_merged_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallMerge,
                                    contentDescription = "Merge Files",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Merge (${uiState.selectedCount})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty state or List of Smali Files
        if (uiState.filteredSources.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PastelTealBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = StrideTeal,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No files match '${uiState.searchQuery}'" else "Ready to Combine",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Choose Smali files, a folder tree, or a ZIP archive to inspect and merge.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                color = Slate500
                            ),
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
            Spacer(modifier = Modifier.height(96.dp)) // Extra padding for floating bottom bar
        }
    }
}

@Composable
private fun ImportTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintBg: Color,
    tintColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                ),
                color = Slate900
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Slate400
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    dotColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Slate400
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = Slate900
            )
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> String.format("%.2f MB", bytes / (1024f * 1024f))
    }
}
