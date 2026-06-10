package com.roadsignai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RoadSignColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = DarkBackground,
    primaryContainer = AccentOrangeVariant,
    onPrimaryContainer = OnDarkSurface,
    secondary = PrimaryBlue,
    onSecondary = DarkBackground,
    secondaryContainer = PrimaryBlueVariant,
    onSecondaryContainer = OnDarkSurface,
    tertiary = StatusGreen,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = StatusRed,
    onError = OnDarkSurface,
    errorContainer = StatusRedDim,
    onErrorContainer = OnDarkSurface,
    outline = OutlineColor,
    outlineVariant = CardBorder
)

@Composable
fun RoadSignAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoadSignColorScheme,
        typography = RoadSignTypography,
        content = content
    )
}
