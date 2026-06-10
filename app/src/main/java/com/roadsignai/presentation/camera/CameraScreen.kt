package com.roadsignai.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadsignai.data.location.LocationTrackingService
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.SignCategory
import com.roadsignai.presentation.theme.*
import com.roadsignai.presentation.theme.SignInformational
import com.roadsignai.presentation.theme.SignMandatory
import com.roadsignai.presentation.theme.SignProhibitory
import com.roadsignai.presentation.theme.SignService
import com.roadsignai.presentation.theme.SignWarning
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * Main camera screen with full-frame detection.
 *
 * Features:
 * - Full-screen camera preview (zone = entire screen)
 * - Center overlay showing detected sign for 3 seconds with visual + TTS
 * - Single log line at bottom when sign is recognized
 * - Compact speed + controls bar
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

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        onEvent(CameraEvent.PermissionsResult(cameraGranted, locationGranted))
    }

    // Start location service
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationTrackingService.start(context)
        }
    }

    // Bind camera
    LaunchedEffect(uiState.hasCameraPermission) {
        if (uiState.hasCameraPermission) {
            cameraController.cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            cameraController.setImageAnalysisAnalyzer(
                Dispatchers.Default.asExecutor()
            ) { imageProxy ->
                onEvent(CameraEvent.ImageAvailable(imageProxy))
            }
            cameraController.bindToLifecycle(lifecycleOwner)
            onEvent(CameraEvent.StartCamera)
        }
    }

    // Cleanup
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
        // ========== 1. Full-screen camera preview ==========
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
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }) {
                        Text("Предоставить доступ")
                    }
                }
            }
        }

        // ========== 2. Center overlay — detected sign card (3 sec) ==========
        AnimatedVisibility(
            visible = uiState.displayedSign != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            uiState.displayedSign?.let { sign ->
                SignDetectedCard(sign = sign)
            }
        }

        // ========== 3. Log line at bottom (only when sign recognized) ==========
        val lastLog = uiState.recentSigns.firstOrNull()
        AnimatedVisibility(
            visible = lastLog != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            lastLog?.let { sign ->
                SignLogLine(sign = sign)
            }
        }

        // ========== 4. Bottom controls bar ==========
        BottomControlsBar(
            speedKmh = uiState.currentSpeedKmh,
            sessionTimeSeconds = uiState.sessionTimeSeconds,
            isMuted = uiState.isMuted,
            onSettingsClick = onSettingsClick,
            onToggleMute = { onEvent(CameraEvent.ToggleMute) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Sign detected card — large center overlay for 3 seconds
// ─────────────────────────────────────────────────────────────

@Composable
private fun SignDetectedCard(sign: RoadSign) {
    val categoryColor = signCategoryColor(sign.category)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurface.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .widthIn(min = 260.dp, max = 340.dp)
            .wrapContentHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Large colored circle representing sign
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                // Category icon/letter
                Text(
                    text = categoryEmoji(sign.category),
                    fontSize = 64.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign code from ПДД РБ
            if (sign.signCode != null) {
                Text(
                    text = "ПДД РБ ${sign.signCode}",
                    style = MaterialTheme.typography.labelLarge,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Sign name (Russian)
            if (sign.signName != null) {
                Text(
                    text = sign.signName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDarkSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = sign.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDarkSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Speed limit value
            if (sign.speedLimitValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = categoryColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${sign.speedLimitValue} км/ч",
                        style = MaterialTheme.typography.headlineSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Confidence
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(sign.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = OnDarkSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Single log line at bottom (visible only when sign recognized)
// ─────────────────────────────────────────────────────────────

@Composable
private fun SignLogLine(sign: RoadSign) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface.copy(alpha = 0.80f),
        tonalElevation = 2.dp
    ) {
        Text(
            text = buildString {
                append("✓ ")
                append(sign.signName ?: sign.label)
                if (sign.signCode != null) append(" [${sign.signCode}]")
                if (sign.speedLimitValue != null) append(" — ${sign.speedLimitValue} км/ч")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = OnDarkSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Bottom controls bar (compact)
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomControlsBar(
    speedKmh: Float,
    sessionTimeSeconds: Long,
    isMuted: Boolean,
    onSettingsClick: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.90f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed display
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (speedKmh > 0) AccentOrange else OnDarkSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${speedKmh.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnDarkSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "км/ч",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = OnDarkSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatDuration(sessionTimeSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnDarkSurfaceVariant
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Mute/Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) StatusRed.copy(alpha = 0.2f) else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Включить звук" else "Выключить звук",
                        tint = if (isMuted) StatusRed else AccentOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = OnDarkSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────

private fun signCategoryColor(category: SignCategory): Color = when {
    category.priority == 1 || category.isProhibitory -> SignProhibitory
    category.priority == 2 -> SignWarning
    category.name.contains("SERVICE") || category.name.contains("FIRST") ||
        category.name.contains("HOSPITAL") || category.name.contains("GAS") ||
        category.name.contains("RESTAURANT") || category.name.contains("HOTEL") ||
        category.name.contains("CAMPING") || category.name.contains("POLICE") ||
        category.name.contains("TOILET") -> SignService
    category.name.contains("INFORMATION") || category.name.contains("PEDESTRIAN") ||
        category.name.contains("MOTORWAY") || category.name.contains("PARKING") ||
        category.name.contains("SETTLEMENT") || category.name.contains("ROAD_NUMBER") -> SignInformational
    else -> SignMandatory
}

/**
 * Returns an emoji representing the sign category for the center overlay.
 */
private fun categoryEmoji(category: SignCategory): String = when {
    category.isProhibitory -> "\u26D4" // ⛔
    category.priority == 1 -> "\u26A0" // ⚠
    category.priority == 2 -> "\u26A0" // ⚠
    category.name in listOf(
        "MOTORWAY", "EXPRESSWAY", "ONE_WAY", "DEAD_END",
        "PEDESTRIAN_CROSSING", "CYCLE_CROSSING", "PEDESTRIAN_UNDERPASS",
        "SETTLEMENT_SIGN", "ROAD_NUMBER", "KILOMETER_MARKER"
    ) -> "\u2139" // ℹ
    category.name in listOf(
        "GAS_STATION", "CHARGING_STATION", "CAR_SERVICE", "CAR_WASH",
        "RESTAURANT", "HOTEL", "CAMPING", "REST_AREA",
        "TELEPHONE", "TOILET", "DRINKING_WATER",
        "FIRST_AID", "HOSPITAL", "POLICE", "LANDMARK"
    ) -> "\uD83D\uDEE5" // 🛥
    category.name == "STOP" -> "\uD83D\uDED1" // 🛑
    category.name == "DIRECTION_MANDATORY" || category.name == "ROUNDABOUT" -> "\u27A1" // ➡
    category.name == "RESIDENTIAL_ZONE" || category.name == "PEDESTRIAN_ZONE" -> "\uD83C\uDFE0" // 🏠
    category.name == "ADDITIONAL_PLATE" -> "\uD83D\uDCCC" // 📌
    category.name.contains("WARNING") -> "\u26A0" // ⚠
    category.name == "SPEED_LIMIT" || category.name == "SPEED_LIMIT_ZONE" -> "\uD83D\uDEA6" // 🚦
    else -> "\uD83D\uDEA7" // 🚧
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
