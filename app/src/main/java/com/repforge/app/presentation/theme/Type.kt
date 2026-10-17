package com.repforge.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

private fun t(w: FontWeight, size: Int, line: Int, track: Float = 0f) =
    TextStyle(fontFamily = Sans, fontWeight = w, fontSize = size.sp, lineHeight = line.sp, letterSpacing = track.sp, fontFeatureSettings = "tnum")

/** Bold only for titles/hero numbers, SemiBold for row titles and buttons, Normal elsewhere. */
val RepForgeTypography = Typography(
    displayLarge = t(FontWeight.Bold, 56, 60, -1f),
    displayMedium = t(FontWeight.Bold, 40, 44, -0.5f),
    displaySmall = t(FontWeight.Bold, 34, 38, -0.8f),
    headlineLarge = t(FontWeight.Bold, 30, 36, -0.5f),
    headlineMedium = t(FontWeight.Bold, 28, 34, -0.3f),
    headlineSmall = t(FontWeight.Bold, 22, 28),
    titleLarge = t(FontWeight.SemiBold, 17, 22),
    titleMedium = t(FontWeight.SemiBold, 16, 22),
    titleSmall = t(FontWeight.SemiBold, 14, 20),
    bodyLarge = t(FontWeight.Normal, 17, 22),
    bodyMedium = t(FontWeight.Normal, 15, 20),
    bodySmall = t(FontWeight.Normal, 13, 18),
    labelLarge = t(FontWeight.SemiBold, 16, 22),
    labelMedium = t(FontWeight.Normal, 12, 16, 0.5f),
    labelSmall = t(FontWeight.Normal, 11, 14),
)
