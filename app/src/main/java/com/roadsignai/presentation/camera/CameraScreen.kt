package com.roadsignai.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadsignai.data.location.LocationTrackingService
import com.roadsignai.presentation.components.*
import com.roadsignai.presentation.theme.*
import java.util.concurrent.TimeUnit

/**
 * Main camera screen showing CameraX preview + overlay + sign cards + controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onSettingsClick: () -> Unit,
    onEvent: (CameraEvent) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }

    // Start location service
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationTrackingService.start(context)
        }
    }

    // Bind camera on permission
    LaunchedEffect(uiState.hasCameraPermission) {
        if (uiState.hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            onEvent(CameraEvent.StartCamera)
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
            LocationTrackingService.stop(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Camera preview
        if (uiState.hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission required overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = OnDarkSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Требуется разрешение камеры",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnDarkSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onEvent(CameraEvent.RequestPermissions) }) {
                        Text("Предоставить доступ")
                    }
                }
            }
        }

        // Sign overlay (bounding boxes on camera preview)
        if (uiState.hasCameraPermission) {
            SignOverlay(
                signs = uiState.detectedSigns,
                previewWidth = 1280,
                previewHeight = 720,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Warning banner (top)
        WarningBanner(
            isVisible = uiState.isWarningVisible,
            message = uiState.warningMessage,
            isCritical = uiState.isWarningCritical,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Approaching zone warning
        ZoneApproachBanner(
            isVisible = uiState.isApproachingWarningVisible,
            message = uiState.approachingWarningMessage,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom panel with signs, zones, and controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.85f))
        ) {
            // Active zones compact view
            if (uiState.activeZones.isNotEmpty()) {
                ZoneMap(
                    activeZones = uiState.activeZones,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Recent signs list
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                color = DarkBackground.copy(alpha = 0.0f)
            ) {
                if (uiState.recentSigns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет обнаруженных знаков",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnDarkSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.recentSigns, key = { it.id }) { sign ->
                            SignCard(
                                sign = sign,
                                distance = "${(sign.confidence * 100).toInt()}%"
                            )
                        }
                    }
                }
            }

            // Bottom controls bar
            BottomControlsBar(
                speedKmh = uiState.currentSpeedKmh,
                sessionTimeSeconds = uiState.sessionTimeSeconds,
                isMuted = uiState.isMuted,
                signCount = uiState.recentSigns.size,
                onSettingsClick = onSettingsClick,
                onToggleMute = { onEvent(CameraEvent.ToggleMute) },
                onClearSigns = { onEvent(CameraEvent.ClearSigns) }
            )
        }

        // Error snackbar
        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { /* dismiss */ }) {
                        Text("OK")
                    }
                }
            ) {
                Text(uiState.errorMessage)
            }
        }
    }
}

@Composable
private fun BottomControlsBar(
    speedKmh: Float,
    sessionTimeSeconds: Long,
    isMuted: Boolean,
    signCount: Int,
    onSettingsClick: () -> Unit,
    onToggleMute: () -> Unit,
    onClearSigns: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed display
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (speedKmh > 0) AccentOrange else OnDarkSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${speedKmh.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnDarkSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "км/ч",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Session timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = OnDarkSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDuration(sessionTimeSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnDarkSurfaceVariant
                )
            }

            // Sign count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Signpost,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$signCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mute/Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) StatusRed.copy(alpha = 0.2f) else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Включить звук" else "Выключить звук",
                        tint = if (isMuted) StatusRed else AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear
                IconButton(
                    onClick = onClearSigns,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Очистить",
                        tint = OnDarkSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = OnDarkSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
