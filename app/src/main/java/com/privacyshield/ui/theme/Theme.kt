package com.privacyshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = BackgroundDark,
    primaryContainer = Color(0xFF003A47),
    onPrimaryContainer = CyanAccent,

    secondary = CyanDim,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF002630),
    onSecondaryContainer = CyanDim,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,

    outline = OutlineDark,
    outlineVariant = Color(0xFF1E1E1E),

    error = RiskHigh,
    onError = Color.White,
    errorContainer = Color(0xFF3C0000),
    onErrorContainer = RiskHigh,

    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
)

@Composable
fun PrivacyShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = PrivacyShieldTypography,
        content = content
    )
}
