package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TechnicalCyan,
    onPrimary = Color(0xFF041E2B),
    primaryContainer = TechnicalCyanContainer,
    onPrimaryContainer = OnTechnicalCyanContainer,
    secondary = TelemetryEmerald,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = TelemetryEmeraldContainer,
    onSecondaryContainer = OnTelemetryEmeraldContainer,
    tertiary = WarningAmber,
    onTertiary = Color(0xFF271202),
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = OnWarningAmberContainer,
    error = CrashCoral,
    onError = Color(0xFF330606),
    errorContainer = CrashCoralContainer,
    onErrorContainer = OnCrashCoralContainer,
    background = CarbonBackground,
    onBackground = SlateTextPrimary,
    surface = CarbonSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    outline = CarbonOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = EmeraldLight,
    onSecondary = Color.White,
    secondaryContainer = EmeraldLightContainer,
    onSecondaryContainer = OnEmeraldLightContainer,
    tertiary = AmberLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberLightContainer,
    onTertiaryContainer = OnAmberLightContainer,
    error = CoralLight,
    onError = Color.White,
    errorContainer = CoralLightContainer,
    onErrorContainer = OnCoralLightContainer,
    background = AlabasterBackground,
    onBackground = LightTextPrimary,
    surface = AlabasterSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = AlabasterOutline
)

@Composable
fun DroidSandboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
