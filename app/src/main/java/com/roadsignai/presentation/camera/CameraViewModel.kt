package com.roadsignai.presentation.camera

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.roadsignai.data.local.preferences.AppSettings
import com.roadsignai.data.local.preferences.SettingsDataStore
import com.roadsignai.data.location.LocationTrackingService
import com.roadsignai.domain.model.RoadSign
import com.roadsignai.domain.model.VehicleState
import com.roadsignai.domain.repository.SignRepository
import com.roadsignai.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the camera screen.
 */
data class CameraUiState(
    val detectedSigns: List<RoadSign> = emptyList(),
    val recentSigns: List<RoadSign> = emptyList(),
    val activeZones: List<com.roadsignai.domain.model.SignZone> = emptyList(),
    val isWarningVisible: Boolean = false,
    val warningMessage: String = "",
    val isWarningCritical: Boolean = false,
    val isApproachingWarningVisible: Boolean = false,
    val approachingWarningMessage: String = "",
    val currentSpeedKmh: Float = 0f,
    val sessionTimeSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null,
    /** Sign currently displayed in center overlay (shows for 3 sec). */
    val displayedSign: RoadSign? = null
)

/**
 * Events that can be sent from the UI to the ViewModel.
 */
sealed class CameraEvent {
    data object RequestPermissions : CameraEvent()
    data class PermissionsResult(val cameraGranted: Boolean, val locationGranted: Boolean) : CameraEvent()
    data object ToggleMute : CameraEvent()
    data object StartCamera : CameraEvent()
    data object StopCamera : CameraEvent()
    data class ImageAvailable(val image: ImageProxy) : CameraEvent()
    data class LocationUpdate(val location: Location) : CameraEvent()
    data object ClearSigns : CameraEvent()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val detectSignsUseCase: DetectSignsUseCase,
    private val calculateZoneUseCase: CalculateZoneUseCase,
    private val checkStopInZoneUseCase: CheckStopInZoneUseCase,
    private val speakSignUseCase: SpeakSignUseCase,
    private val signRepository: SignRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val previousSigns = mutableListOf<RoadSign>()
    private val processedSignIds = mutableSetOf<String>()
    private var settings = AppSettings()
    private var lastLocation: Location? = null
    private var frameCount = 0

    // Session timer
    private var sessionStartTime = 0L
    private var timerJob: Job? = null

    // Vehicle state tracking
    private var vehicleState = VehicleState()

    // Location tracking subscription
    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { newSettings ->
                settings = newSettings
                speakSignUseCase.setMinInterval(newSettings.ttsIntervalMs)
                updatePermissions()
            }
        }

        startSessionTimer()
        observeLocation()
    }

    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.RequestPermissions -> {
                updatePermissions()
                if (_uiState.value.hasCameraPermission && _uiState.value.hasLocationPermission) {
                    startDetection()
                }
            }
            is CameraEvent.PermissionsResult -> {
                _uiState.update {
                    it.copy(
                        hasCameraPermission = event.cameraGranted,
                        hasLocationPermission = event.locationGranted
                    )
                }
                if (event.cameraGranted && event.locationGranted) {
                    LocationTrackingService.start(getApplication())
                }
            }
            is CameraEvent.ToggleMute -> {
                _uiState.update { it.copy(isMuted = !it.isMuted) }
                if (_uiState.value.isMuted) {
                    speakSignUseCase.stop()
                }
            }
            is CameraEvent.StartCamera -> startDetection()
            is CameraEvent.StopCamera -> stopDetection()
            is CameraEvent.ImageAvailable -> processImage(event.image)
            is CameraEvent.LocationUpdate -> updateLocation(event.location)
            is CameraEvent.ClearSigns -> {
                viewModelScope.launch {
                    previousSigns.clear()
                    processedSignIds.clear()
                    signRepository.clearAll()
                    _uiState.update { it.copy(detectedSigns = emptyList(), recentSigns = emptyList()) }
                }
            }
        }
    }

    private fun updatePermissions() {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED,
                hasLocationPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    private fun startSessionTimer() {
        sessionStartTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
                _uiState.update { it.copy(sessionTimeSeconds = elapsed) }
            }
        }
    }

    private fun observeLocation() {
        locationJob = viewModelScope.launch {
            LocationTrackingService.currentLocation.collect { location ->
                if (location != null) {
                    onEvent(CameraEvent.LocationUpdate(location))
                }
            }
        }
    }

    /** Timer Job to clear displayed sign after 3 seconds. */
    private var displayTimerJob: Job? = null

    private fun launchDisplayClearTimer() {
        displayTimerJob?.cancel()
        displayTimerJob = viewModelScope.launch {
            delay(3000L)
            _uiState.update { it.copy(displayedSign = null) }
        }
    }

    private fun startDetection() {
        // Image analysis analyzer is set up in CameraScreen via
        // cameraController.setImageAnalysisAnalyzer(). Nothing else needed here.
    }

    private fun stopDetection() {
        speakSignUseCase.stop()
    }

    private fun processImage(imageProxy: ImageProxy) {
        frameCount++
        // Skip 2 out of 3 frames for performance
        if (frameCount % 3 != 0) {
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            try {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    val result = detectSignsUseCase(inputImage)

                    if (result.signs.isNotEmpty()) {
                        handleDetectedSigns(result.signs)
                    }

                    // Check for errors
                    if (result.error != null) {
                        _uiState.update { it.copy(errorMessage = result.error) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                imageProxy.close()
            }
        }
    }

    private suspend fun handleDetectedSigns(signs: List<RoadSign>) {
        val newSigns = mutableListOf<RoadSign>()

        for (sign in signs) {
            // Skip if confidence is below display threshold
            if (sign.confidence < settings.minConfidence) continue

            // Skip consecutive duplicates
            val lastSign = previousSigns.lastOrNull()
            if (lastSign != null && lastSign.category == sign.category) continue

            newSigns.add(sign)
            previousSigns.add(sign)

            // Save to repository if logging enabled
            if (settings.saveLogs) {
                signRepository.saveSign(sign)
            }

            // Speak if above speak threshold and not muted
            if (!_uiState.value.isMuted &&
                sign.confidence >= settings.speakConfidence &&
                !processedSignIds.contains(sign.id)
            ) {
                processedSignIds.add(sign.id)
                speakSignUseCase(sign, settings.ttsIntervalMs)
            }

            // Calculate zone for prohibitory signs
            if (sign.category.isProhibitory && lastLocation != null) {
                val zoneResult = calculateZoneUseCase(
                    sign = sign,
                    currentLocation = lastLocation!!,
                    defaultSpeedZoneDistance = settings.speedZoneDistance,
                    defaultParkingZoneDistance = settings.parkingZoneDistance
                )

                if (zoneResult.zoneEnded && !_uiState.value.isMuted) {
                    speakSignUseCase.speakZoneEnded()
                }
            }
        }

        // Update UI
        val recentSigns = previousSigns.takeLast(10).reversed()
        val activeZones = calculateZoneUseCase.getActiveZones()
        _uiState.update {
            it.copy(
                detectedSigns = signs.filter { s -> s.confidence >= settings.minConfidence },
                recentSigns = recentSigns,
                activeZones = activeZones
            )
        }

        // Show first recognized sign in center overlay for 3 seconds
        val signToDisplay = newSigns.firstOrNull { it.confidence >= settings.speakConfidence }
        if (signToDisplay != null) {
            _uiState.update { it.copy(displayedSign = signToDisplay) }
            // Auto-clear after 3 seconds
            launchDisplayClearTimer()
        }

        // Check for stop-in-zone violation
        if (lastLocation != null && activeZones.isNotEmpty()) {
            val stopResult = checkStopInZoneUseCase(vehicleState, activeZones)
            handleStopCheckResult(stopResult)
        }
    }

    private fun updateLocation(location: Location) {
        lastLocation = location

        // Update speed and stopped state
        val currentTime = System.currentTimeMillis()
        val newStoppedSince = if (location.speed < 3f) {
            if (vehicleState.stoppedSinceTimestamp == 0L) currentTime
            else vehicleState.stoppedSinceTimestamp
        } else {
            0L
        }

        vehicleState = vehicleState.copy(
            speedKmh = location.speed * 3.6f, // m/s to km/h
            location = location,
            isStopped = VehicleState.isVehicleStopped(
                location.speed * 3.6f,
                newStoppedSince
            ),
            stoppedSinceTimestamp = newStoppedSince,
            accuracy = location.accuracy
        )

        viewModelScope.launch {
            signRepository.saveVehicleState(vehicleState)
            _uiState.update { it.copy(currentSpeedKmh = vehicleState.speedKmh) }
        }
    }

    private suspend fun handleStopCheckResult(
        result: CheckStopInZoneUseCase.StopCheckResult
    ) {
        when {
            result.isStoppedInZone -> {
                _uiState.update {
                    it.copy(
                        isWarningVisible = true,
                        warningMessage = result.warningMessage ?: "",
                        isWarningCritical = true
                    )
                }

                if (!_uiState.value.isMuted) {
                    speakSignUseCase.speakWarning(
                        "Внимание! Остановка в зоне действия знака! Немедленно покиньте зону!",
                        flush = true
                    )

                    // Vibrate
                    triggerVibration(500L)
                }
            }
            result.isApproachingZone -> {
                _uiState.update {
                    it.copy(
                        isApproachingWarningVisible = true,
                        approachingWarningMessage = result.warningMessage ?: ""
                    )
                }

                if (!_uiState.value.isMuted) {
                    speakSignUseCase.speakWarning(
                        result.warningMessage ?: "Приближается зона действия знака",
                        flush = false
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isWarningVisible = false,
                        isApproachingWarningVisible = false
                    )
                }
            }
        }
    }

    private fun triggerVibration(durationMs: Long) {
        if (!settings.vibrationEnabled) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (_: Exception) {
            // Ignore vibration errors
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        displayTimerJob?.cancel()
        locationJob?.cancel()
        speakSignUseCase.shutdown()
    }
}
