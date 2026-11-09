package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VpnRedPrimary,
    onPrimary = Color.White,
    primaryContainer = VpnRedContainerDark,
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = VpnRedLight,
    onSecondary = Color.White,
    tertiary = VpnCyanAccent,
    background = VpnDarkBackground,
    onBackground = VpnDarkOnSurface,
    surface = VpnDarkSurface,
    onSurface = VpnDarkOnSurface,
    surfaceVariant = VpnDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = VpnDarkCardStroke
)

private val LightColorScheme = lightColorScheme(
    primary = VpnRedPrimary,
    onPrimary = Color.White,
    primaryContainer = VpnRedContainerLight,
    onPrimaryContainer = VpnRedDeep,
    secondary = VpnRedDark,
    onSecondary = Color.White,
    tertiary = VpnRedLight,
    background = VpnLightBackground,
    onBackground = VpnLightOnSurface,
    surface = VpnLightSurface,
    onSurface = VpnLightOnSurface,
    surfaceVariant = VpnLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = VpnLightCardStroke
)

@Composable
fun MutigandaVpnTheme(
    themePreference: String = "Dark",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
