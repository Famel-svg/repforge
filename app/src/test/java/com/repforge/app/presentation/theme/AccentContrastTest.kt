package com.repforge.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.repforge.app.data.Accent
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentContrastTest {
    @Test fun everyAccentMeetsAaInBothSchemes() {
        for (accent in Accent.entries) for (dark in listOf(true, false)) {
            val c = (if (dark) RepForgeDarkColors else RepForgeLightColors).withAccent(accent, dark)
            fun check(what: String, a: Color, b: Color) = assertTrue("$accent dark=$dark $what", contrast(a, b) >= 4.5f)
            check("onPrimary", c.primary, c.onPrimary)
            check("onPrimaryContainer", c.primaryContainer, c.onPrimaryContainer)
            check("primary on background", c.primary, c.background)
        }
    }
}
