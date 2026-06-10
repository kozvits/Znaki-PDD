package com.roadsignai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsignai.data.local.preferences.AppSettings
import com.roadsignai.data.local.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val ttsEnabled: Boolean = true,
    val ttsVolume: Float = 0.8f,
    val minConfidence: Float = 0.6f,
    val speakConfidence: Float = 0.75f,
    val ttsIntervalMs: Long = 2000L,
    val speedZoneDistance: Float = 500f,
    val parkingZoneDistance: Float = 50f,
    val vibrationEnabled: Boolean = true,
    val showMap: Boolean = true,
    val saveLogs: Boolean = false
)

sealed class SettingsEvent {
    data class SetTtsEnabled(val enabled: Boolean) : SettingsEvent()
    data class SetTtsVolume(val volume: Float) : SettingsEvent()
    data class SetMinConfidence(val confidence: Float) : SettingsEvent()
    data class SetSpeakConfidence(val confidence: Float) : SettingsEvent()
    data class SetTtsInterval(val intervalMs: Long) : SettingsEvent()
    data class SetSpeedZoneDistance(val distance: Float) : SettingsEvent()
    data class SetParkingZoneDistance(val distance: Float) : SettingsEvent()
    data class SetVibrationEnabled(val enabled: Boolean) : SettingsEvent()
    data class SetShowMap(val show: Boolean) : SettingsEvent()
    data class SetSaveLogs(val save: Boolean) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { appSettings ->
                _settingsState.value = appSettings.toSettingsState()
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetTtsEnabled -> {
                    _settingsState.update { it.copy(ttsEnabled = event.enabled) }
                    settingsDataStore.updateSetting("tts_enabled", event.enabled)
                }
                is SettingsEvent.SetTtsVolume -> {
                    _settingsState.update { it.copy(ttsVolume = event.volume) }
                    settingsDataStore.updateSetting("tts_volume", event.volume)
                }
                is SettingsEvent.SetMinConfidence -> {
                    _settingsState.update { it.copy(minConfidence = event.confidence) }
                    settingsDataStore.updateSetting("min_confidence", event.confidence)
                }
                is SettingsEvent.SetSpeakConfidence -> {
                    _settingsState.update { it.copy(speakConfidence = event.confidence) }
                    settingsDataStore.updateSetting("speak_confidence", event.confidence)
                }
                is SettingsEvent.SetTtsInterval -> {
                    _settingsState.update { it.copy(ttsIntervalMs = event.intervalMs) }
                    settingsDataStore.updateSetting("tts_interval", event.intervalMs.toInt())
                }
                is SettingsEvent.SetSpeedZoneDistance -> {
                    _settingsState.update { it.copy(speedZoneDistance = event.distance) }
                    settingsDataStore.updateSetting("speed_zone_distance", event.distance.toInt())
                }
                is SettingsEvent.SetParkingZoneDistance -> {
                    _settingsState.update { it.copy(parkingZoneDistance = event.distance) }
                    settingsDataStore.updateSetting("parking_zone_distance", event.distance.toInt())
                }
                is SettingsEvent.SetVibrationEnabled -> {
                    _settingsState.update { it.copy(vibrationEnabled = event.enabled) }
                    settingsDataStore.updateSetting("vibration_enabled", event.enabled)
                }
                is SettingsEvent.SetShowMap -> {
                    _settingsState.update { it.copy(showMap = event.show) }
                    settingsDataStore.updateSetting("show_map", event.show)
                }
                is SettingsEvent.SetSaveLogs -> {
                    _settingsState.update { it.copy(saveLogs = event.save) }
                    settingsDataStore.updateSetting("save_logs", event.save)
                }
            }
        }
    }
}

private fun AppSettings.toSettingsState() = SettingsState(
    ttsEnabled = ttsEnabled,
    ttsVolume = ttsVolume,
    minConfidence = minConfidence,
    speakConfidence = speakConfidence,
    ttsIntervalMs = ttsIntervalMs,
    speedZoneDistance = speedZoneDistance,
    parkingZoneDistance = parkingZoneDistance,
    vibrationEnabled = vibrationEnabled,
    showMap = showMap,
    saveLogs = saveLogs
)
