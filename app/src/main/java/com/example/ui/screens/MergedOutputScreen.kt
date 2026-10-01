package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HeaderFormat
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.SmaliMergerUiState
import com.example.ui.viewmodel.SmaliMergerViewModel

@Composable
fun MergedOutputScreen(
    viewModel: SmaliMergerViewModel,
    uiState: SmaliMergerUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val hasOutput = uiState.mergedOutput.isNotBlank()

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveMergedOutputToUri(it, context)
            Toast.makeText(context, "Saved output to storage!", Toast.LENGTH_LONG).show()
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("merged_output_screen")
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Redesigned Top Section: Unified Material 3 Expressive Output Control Deck
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header status & file metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Output Document",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasOutput) {
                                    "${uiState.mergedFileCount} files combined • ${formatBytes(uiState.mergedSizeBytes)}"
                                } else {
                                    "Configure format & export options"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (hasOutput) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ".TXT READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = primaryColor
                            )
                        }
                    }
                }

                // Row 1: Glitch-Free Smooth Sliding Segmented Format Selector
                val formats = listOf(
                    HeaderFormat.PATH_THEN_NAME to "Path & Name",
                    HeaderFormat.FILE_ONLY to "File Header",
                    HeaderFormat.PATH_DETAILED to "Detailed"
                )
                val selectedIndex = formats.indexOfFirst { it.first == uiState.headerFormat }.coerceAtLeast(0)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    val totalWidth = maxWidth
                    val tabWidth = totalWidth / formats.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * selectedIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "indicator_offset"
                    )

                    // Single sliding pill indicator: high-contrast active fill, prominent border & smooth slide
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .background(primaryColor.copy(alpha = 0.18f))
                            .border(1.5.dp, primaryColor, RoundedCornerShape(11.dp))
                    )

                    // Foreground clickable tabs
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        formats.forEachIndexed { index, (format, label) ->
                            val isSelected = index == selectedIndex
                            val animatedTextColor by animateColorAsState(
                                targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "tab_text_color"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable { viewModel.setHeaderFormat(format) }
                                    .testTag("format_chip_${format.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active Format",
                                            tint = primaryColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        ),
                                        color = animatedTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Format Explanation Preview Banner: shows exactly what the selected format outputs
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (uiState.headerFormat) {
                                HeaderFormat.PATH_THEN_NAME -> "Header: ===== Path: {path} ===== \\n File: {name}"
                                HeaderFormat.FILE_ONLY -> "Header: ===== {name} ====="
                                HeaderFormat.PATH_DETAILED -> "Header: ===== Path: {path} ===== | Origin: {zip/dir}"
                            },
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 2: Perfectly Even Action Buttons Bar (Equal weights, equal heights, identical disabled colors)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                    val buttonShape = RoundedCornerShape(12.dp)
                    val uniformDisabledContainer = MaterialTheme.colorScheme.surfaceVariant
                    val uniformDisabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

                    // 1. Save Button (Equal weight 1f)
                    Button(
                        onClick = { saveFileLauncher.launch("merged_smali_output.txt") },
                        enabled = hasOutput,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White,
                            disabledContainerColor = uniformDisabledContainer,
                            disabledContentColor = uniformDisabledContent
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = buttonModifier.testTag("save_to_storage_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }

                    // 2. Copy Button (Equal weight 1f)
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(uiState.mergedOutput))
                            Toast.makeText(context, "Copied output to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        enabled = hasOutput,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = uniformDisabledContainer,
                            disabledContentColor = uniformDisabledContent
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = buttonModifier.testTag("copy_output_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }

                    // 3. Share Button (Equal weight 1f)
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Merged Smali Output")
                                putExtra(Intent.EXTRA_TEXT, uiState.mergedOutput)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Merged Smali"))
                        },
                        enabled = hasOutput,
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = uniformDisabledContainer,
                            disabledContentColor = uniformDisabledContent
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = buttonModifier.testTag("share_output_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Code Viewer Area - Light / Dark / AMOLED Material UI
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            if (!hasOutput) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatAlignLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files selected to merge",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select files from the Sources tab to see output here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.5.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Document Tab Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "merged_smali_output.txt",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${uiState.mergedOutput.lines().size} lines",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val allLines = uiState.mergedOutput.lines()
                    val isVeryLarge = allLines.size > 2000
                    val displayText = if (isVeryLarge) {
                        allLines.take(1500).joinToString("\n")
                    } else {
                        uiState.mergedOutput
                    }

                    if (isVeryLarge) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Previewing first 1,500 of ${allLines.size} lines for smooth scrolling. Complete file exported on Save/Copy/Share.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = primaryColor
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = displayText,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp)) // Padding for floating bottom bar
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> String.format("%.2f MB", bytes / (1024f * 1024f))
    }
}
