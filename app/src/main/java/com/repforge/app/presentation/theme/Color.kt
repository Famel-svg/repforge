package com.repforge.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Black canvas, neutral grey surface ramp, one green accent. Text pairs verified in AccentContrastTest.

val RepForgeDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF30D158), onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1F3927), onPrimaryContainer = Color(0xFF30D158),
    inversePrimary = Color(0xFF1F7A35),
    secondary = Color(0xFFFF9F0A), onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF3A2606), onSecondaryContainer = Color(0xFFFF9F0A),
    tertiary = Color(0xFFFFD60A), onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF3A3000), onTertiaryContainer = Color(0xFFFFD60A),
    background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000), onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF98989F),
    surfaceTint = Color(0xFF30D158),
    inverseSurface = Color(0xFFF2F2F7), inverseOnSurface = Color(0xFF000000),
    error = Color(0xFFFF453A), onError = Color(0xFF000000),
    errorContainer = Color(0xFF3A1210), onErrorContainer = Color(0xFFFF453A),
    outline = Color(0xFF8E8E93), outlineVariant = Color(0xFF38383A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2C2E), surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000), surfaceContainerLow = Color(0xFF0E0E10),
    surfaceContainer = Color(0xFF1C1C1E), surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
)

val RepForgeLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F7A35), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF3E2), onPrimaryContainer = Color(0xFF145226),
    inversePrimary = Color(0xFF30D158),
    secondary = Color(0xFFA85A00), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE3C2), onSecondaryContainer = Color(0xFF7A4300),
    tertiary = Color(0xFF806600), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1B8), onTertiaryContainer = Color(0xFF5C4800),
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF000000),
    surface = Color(0xFFF2F2F7), onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA), onSurfaceVariant = Color(0xFF5F5F64),
    surfaceTint = Color(0xFF1F7A35),
    inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFD70015), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF8C000E),
    outline = Color(0xFF6E6E73), outlineVariant = Color(0xFFC6C6C8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFD1D1D6),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6),
)
