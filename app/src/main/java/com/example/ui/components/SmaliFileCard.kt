package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmaliSource
import com.example.data.model.SourceOrigin
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PastelAmberBg
import com.example.ui.theme.PastelAmberText
import com.example.ui.theme.PastelBlueBg
import com.example.ui.theme.PastelBlueText
import com.example.ui.theme.PastelTealBg
import com.example.ui.theme.PastelTealText
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900

/**
 * Modern, clean file card matching Stride's spacious card design with modern action buttons.
 */
@Composable
fun SmaliFileCard(
    source: SmaliSource,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val (iconBg, iconColor, badgeBg, badgeTextColor) = when {
        source.origin == SourceOrigin.ZIP_ARCHIVE -> Quadruple(
            PastelBlueBg, PastelBlueText, PastelBlueBg, PastelBlueText
        )
        !source.isSmaliExtension -> Quadruple(
            PastelAmberBg, PastelAmberText, PastelAmberBg, PastelAmberText
        )
        else -> Quadruple(
            PastelTealBg, PastelTealText, PastelTealBg, PastelTealText
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                color = if (source.isSelected) primaryColor.copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("file_card_${source.id}"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (source.isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSelect() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle icon with gentle pastel tint
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        source.origin == SourceOrigin.ZIP_ARCHIVE -> Icons.Default.FolderZip
                        !source.isSmaliExtension -> Icons.Default.DataObject
                        else -> Icons.Default.Terminal
                    },
                    contentDescription = "File Type",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = source.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Pastel origin pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = source.displayBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = badgeTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = source.relativePath,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats pill
                val sizeFormatted = formatBytes(source.sizeBytes)
                Text(
                    text = "$sizeFormatted • ${source.lineCount} lines",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Modern Action Buttons with Updated Sleek Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Modern Preview Code Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPreview() }
                        .testTag("preview_btn_${source.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Preview Code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Modern Remove File Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                        .clickable { onRemove() }
                        .testTag("remove_btn_${source.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove File",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Checkbox(
                    checked = source.isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryColor,
                        uncheckedColor = Slate300
                    ),
                    modifier = Modifier.testTag("checkbox_${source.id}")
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> String.format("%.2f MB", bytes / (1024f * 1024f))
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
