package com.example.nutritracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006E2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9AF7A5),
    onPrimaryContainer = Color(0xFF002109),
    secondary = Color(0xFF506352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D2),
    onSecondaryContainer = Color(0xFF0E1F12),
    tertiary = Color(0xFF3A656E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBDEBF5),
    onTertiaryContainer = Color(0xFF001F26),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF0F1EB),
    inversePrimary = Color(0xFF7EDA8B),
    surfaceDim = Color(0xFFDCDDD7),
    surfaceBright = Color(0xFFFCFDF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F7F1),
    surfaceContainer = Color(0xFFF0F1EB),
    surfaceContainerHigh = Color(0xFFEAECE6),
    surfaceContainerHighest = Color(0xFFE5E6E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7EDA8B),
    onPrimary = Color(0xFF003913),
    primaryContainer = Color(0xFF00531F),
    onPrimaryContainer = Color(0xFF9AF7A5),
    secondary = Color(0xFFB7CCB7),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF394B3B),
    onSecondaryContainer = Color(0xFFD3E8D2),
    tertiary = Color(0xFFA2CED8),
    onTertiary = Color(0xFF01363F),
    tertiaryContainer = Color(0xFF204D56),
    onTertiaryContainer = Color(0xFFBDEBF5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    inverseSurface = Color(0xFFE2E3DD),
    inverseOnSurface = Color(0xFF2F312D),
    inversePrimary = Color(0xFF006E2B),
    surfaceDim = Color(0xFF1A1C19),
    surfaceBright = Color(0xFF393B37),
    surfaceContainerLowest = Color(0xFF0F1210),
    surfaceContainerLow = Color(0xFF1A1C19),
    surfaceContainer = Color(0xFF1E201D),
    surfaceContainerHigh = Color(0xFF282B27),
    surfaceContainerHighest = Color(0xFF333632)
)

@Composable
fun NutriTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
