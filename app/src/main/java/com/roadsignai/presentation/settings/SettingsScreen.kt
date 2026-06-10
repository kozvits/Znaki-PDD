package com.roadsignai.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadsignai.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnDarkSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = OnDarkSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // TTS section
            SettingsSectionHeader("Голосовые подсказки (TTS)")

            SettingsSwitch(
                title = "TTS включён",
                subtitle = "Голосовое озвучивание знаков",
                checked = state.ttsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetTtsEnabled(it)) }
            )

            if (state.ttsEnabled) {
                SettingsSlider(
                    title = "Громкость TTS",
                    value = state.ttsVolume,
                    onValueChange = { onEvent(SettingsEvent.SetTtsVolume(it)) },
                    valueRange = 0f..1f,
                    formatValue = { "%.0f%%".format(it * 100) }
                )

                SettingsSlider(
                    title = "Интервал озвучивания",
                    value = state.ttsIntervalMs / 1000f,
                    onValueChange = { onEvent(SettingsEvent.SetTtsInterval((it * 1000).toLong())) },
                    valueRange = 1f..5f,
                    formatValue = { "%.0f сек".format(it) }
                )
            }

            HorizontalDivider(
                color = CardBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Detection section
            SettingsSectionHeader("Детекция знаков")

            SettingsSlider(
                title = "Мин. уверенность для отображения",
                value = state.minConfidence,
                onValueChange = { onEvent(SettingsEvent.SetMinConfidence(it)) },
                valueRange = 0.3f..0.95f,
                formatValue = { "%.0f%%".format(it * 100) }
            )

            SettingsSlider(
                title = "Порог для озвучивания",
                value = state.speakConfidence,
                onValueChange = { onEvent(SettingsEvent.SetSpeakConfidence(it)) },
                valueRange = 0.3f..0.95f,
                formatValue = { "%.0f%%".format(it * 100) }
            )

            HorizontalDivider(
                color = CardBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Zones section
            SettingsSectionHeader("Зоны действия")

            SettingsSlider(
                title = "Зона по умолчанию (скорость)",
                subtitle = "Максимальное расстояние для знаков скорости",
                value = state.speedZoneDistance,
                onValueChange = { onEvent(SettingsEvent.SetSpeedZoneDistance(it)) },
                valueRange = 100f..2000f,
                steps = 18,
                formatValue = { "%.0f м".format(it) }
            )

            SettingsSlider(
                title = "Зона по умолчанию (стоянка)",
                subtitle = "Максимальное расстояние для знаков стоянки",
                value = state.parkingZoneDistance,
                onValueChange = { onEvent(SettingsEvent.SetParkingZoneDistance(it)) },
                valueRange = 10f..200f,
                steps = 18,
                formatValue = { "%.0f м".format(it) }
            )

            HorizontalDivider(
                color = CardBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Behavior section
            SettingsSectionHeader("Поведение")

            SettingsSwitch(
                title = "Вибрация при нарушении",
                subtitle = "Виброотклик при остановке в зоне запрета",
                checked = state.vibrationEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetVibrationEnabled(it)) }
            )

            SettingsSwitch(
                title = "Показывать карту зон",
                subtitle = "Отображение активных зон на главном экране",
                checked = state.showMap,
                onCheckedChange = { onEvent(SettingsEvent.SetShowMap(it)) }
            )

            SettingsSwitch(
                title = "Сохранять логи",
                subtitle = "Сохранять историю обнаруженных знаков в базу данных",
                checked = state.saveLogs,
                onCheckedChange = { onEvent(SettingsEvent.SetSaveLogs(it)) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RoadSignAI v1.0.0",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnDarkSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Детектор дорожных знаков на базе ML Kit",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = AccentOrange,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnDarkSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AccentOrange.copy(alpha = 0.5f),
                    checkedThumbColor = AccentOrange
                )
            )
        }
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    subtitle: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    formatValue: (Float) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnDarkSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnDarkSurfaceVariant
                        )
                    }
                }
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = AccentOrange,
                    activeTrackColor = AccentOrange,
                    inactiveTrackColor = DarkSurfaceVariant
                )
            )
        }
    }
}
