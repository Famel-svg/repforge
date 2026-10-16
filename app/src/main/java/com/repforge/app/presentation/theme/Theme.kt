package com.repforge.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import com.repforge.app.data.Accent
import com.repforge.app.data.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme

@Composable
fun RepForgeTheme(
    themeMode: ThemeMode = ThemeMode.System,
    accent: Accent = Accent.Ember,
    darkTheme: Boolean = when (themeMode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Light -> false; ThemeMode.Dark -> true },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= 31 -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RepForgeDarkColors.withAccent(accent, true)
        else -> RepForgeLightColors.withAccent(accent, false)
    }
    MaterialTheme(
        colorScheme = colors,
        typography = RepForgeTypography,
        shapes = RepForgeShapes,
        content = content,
    )
}

private fun Color.mix(other: Color, t: Float) = lerp(this, other, t)

/** Replaces the primary roles with accent-derived ones; Ember keeps the hand-tuned brand values. */
fun ColorScheme.withAccent(accent: Accent, dark: Boolean): ColorScheme {
    if (accent == Accent.Ember) return this
    return if (dark) {
        val p = Color(accent.dark)
        copy(
            primary = p, onPrimary = listOf(Color(0xFF101010), Color.White).maxBy { contrast(p, it) },
            primaryContainer = p.mix(Color.Black, 0.6f), onPrimaryContainer = p.mix(Color.White, 0.8f),
            inversePrimary = Color(accent.light), surfaceTint = p
        )
    } else {
        val p = Color(accent.light)
        copy(
            primary = p, onPrimary = Color.White,
            primaryContainer = p.mix(Color.White, 0.85f), onPrimaryContainer = p.mix(Color.Black, 0.7f),
            inversePrimary = Color(accent.dark), surfaceTint = p
        )
    }
}

/** WCAG 2.x contrast ratio of two opaque colors. */
fun contrast(a: Color, b: Color): Float {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05f) / (lo + 0.05f)
}
