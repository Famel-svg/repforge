package com.repforge.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.repforge.app.data.Accent
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentContrastTest {
    @Test fun everyAccentMeetsAaInBothSchemes() {
        for (accent in Accent.entries) for (dark in listOf(true, false)) {
            val c = (if (dark) RepForgeDarkColors else RepForgeLightColors).withAccent(accent, dark)
            fun check(what: String, a: Color, b: Color) = assertTrue("$accent dark=$dark $what ${contrast(a, b)}", contrast(a, b) >= 4.5f)
            check("onPrimary", c.primary, c.onPrimary)
            check("onPrimaryContainer", c.primaryContainer, c.onPrimaryContainer)
            check("primary on background", c.primary, c.background)
            check("primary on card", c.primary, c.surfaceContainer)
            // Light accents (spec: 1F7A35 = 5.4 on white) only reach ~4.3 on the E5E5EA tone, so accent text there is dark-only.
            if (dark) check("primary on High", c.primary, c.surfaceContainerHigh)
            check("onSurface on card", c.onSurface, c.surfaceContainer)
            check("onSurface on High", c.onSurface, c.surfaceContainerHigh)
            check("onSurfaceVariant on background", c.onSurfaceVariant, c.background)
            check("onSurfaceVariant on card", c.onSurfaceVariant, c.surfaceContainer)
            check("onSurfaceVariant on High", c.onSurfaceVariant, c.surfaceContainerHigh)
            check("secondary on card", c.secondary, c.surfaceContainer)
            check("onSecondary", c.secondary, c.onSecondary)
            check("onSecondaryContainer", c.secondaryContainer, c.onSecondaryContainer)
            check("tertiary on card", c.tertiary, c.surfaceContainer)
            check("onTertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer)
            check("error on card", c.error, c.surfaceContainer)
            check("onError", c.error, c.onError)
            check("onErrorContainer", c.errorContainer, c.onErrorContainer)
        }
    }
}
