package com.roadsignai.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_preferences"
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")
        val MIN_CONFIDENCE = floatPreferencesKey("min_confidence")
        val SPEAK_CONFIDENCE = floatPreferencesKey("speak_confidence")
        val TTS_INTERVAL = intPreferencesKey("tts_interval")
        val SPEED_ZONE_DISTANCE = intPreferencesKey("speed_zone_distance")
        val PARKING_ZONE_DISTANCE = intPreferencesKey("parking_zone_distance")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SHOW_MAP = booleanPreferencesKey("show_map")
        val SAVE_LOGS = booleanPreferencesKey("save_logs")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            ttsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
            ttsVolume = prefs[Keys.TTS_VOLUME] ?: 0.8f,
            minConfidence = prefs[Keys.MIN_CONFIDENCE] ?: 0.6f,
            speakConfidence = prefs[Keys.SPEAK_CONFIDENCE] ?: 0.75f,
            ttsIntervalMs = (prefs[Keys.TTS_INTERVAL] ?: 2000).toLong(),
            speedZoneDistance = (prefs[Keys.SPEED_ZONE_DISTANCE] ?: 500).toFloat(),
            parkingZoneDistance = (prefs[Keys.PARKING_ZONE_DISTANCE] ?: 50).toFloat(),
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            showMap = prefs[Keys.SHOW_MAP] ?: true,
            saveLogs = prefs[Keys.SAVE_LOGS] ?: false
        )
    }

    suspend fun updateSetting(key: String, value: Any) {
        context.settingsDataStore.edit { prefs ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Int -> prefs[intPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
                is String -> prefs[stringPreferencesKey(key)] = value
            }
        }
    }

    suspend fun updateAll(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.TTS_ENABLED] = settings.ttsEnabled
            prefs[Keys.TTS_VOLUME] = settings.ttsVolume
            prefs[Keys.MIN_CONFIDENCE] = settings.minConfidence
            prefs[Keys.SPEAK_CONFIDENCE] = settings.speakConfidence
            prefs[Keys.TTS_INTERVAL] = settings.ttsIntervalMs.toInt()
            prefs[Keys.SPEED_ZONE_DISTANCE] = settings.speedZoneDistance.toInt()
            prefs[Keys.PARKING_ZONE_DISTANCE] = settings.parkingZoneDistance.toInt()
            prefs[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[Keys.SHOW_MAP] = settings.showMap
            prefs[Keys.SAVE_LOGS] = settings.saveLogs
        }
    }
}

data class AppSettings(
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
