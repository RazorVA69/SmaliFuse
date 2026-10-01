package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.SmaliMergerUiState

/**
 * Expressive Modal Progress Dialog with Traveling Wave/Squiggle Animation.
 * Prevents UI freezes and ANR while loading large archives or scanning large directory trees.
 */
@Composable
fun ProgressWaveDialog(
    uiState: SmaliMergerUiState
) {
    if (!uiState.isProcessing) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Dialog(
        onDismissRequest = { /* Modal: keep active until extraction/parsing completes */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("progress_wave_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Energetic Brand Glyph Squircle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.18f),
                                    secondaryColor.copy(alpha = 0.12f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = "Extracting Archive",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = uiState.processingTitle.ifBlank { "Extracting Smali Files..." },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / current item indicator
                Text(
                    text = uiState.processingSubtitle.ifBlank { "Decompressing and parsing entries..." },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Lively Travelling Sine Wave Squiggle Loader
                SnakeSquiggleProgressBar(
                    progress = if (uiState.processingProgress in 0.01f..0.99f) uiState.processingProgress else 0.5f,
                    isIndeterminate = uiState.processingProgress <= 0f || uiState.processingProgress >= 1f,
                    strokeWidth = 4.dp,
                    amplitude = 5.dp,
                    wavelength = 24.dp,
                    brush = Brush.horizontalGradient(
                        listOf(primaryColor, secondaryColor, primaryColor)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Smooth activity micro-label
                Text(
                    text = "Background decompression in progress • Please wait",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
