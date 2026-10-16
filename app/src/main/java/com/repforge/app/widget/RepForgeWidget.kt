package com.repforge.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.repforge.app.MainActivity
import com.repforge.app.R
import com.repforge.app.RepForgeApplication
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// Brand palette from presentation/theme/Color.kt: (light, dark).
private val Surface = ColorProvider(day = Color(0xFFF7F8FA), night = Color(0xFF0B0F14))
private val OnSurface = ColorProvider(day = Color(0xFF14181D), night = Color(0xFFE6EAF0))
private val Muted = ColorProvider(day = Color(0xFF4A5560), night = Color(0xFFB4BECA))
private val Accent = ColorProvider(day = Color(0xFFB04A00), night = Color(0xFFFF8A3D))
private val OnAccent = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF2B1000))

private class WidgetData(val summary: DashboardSummary, val lastSheet: TrainingSheet?)

class RepForgeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as RepForgeApplication).repository
        // Room flows query off the main thread; first() takes one snapshot. Sheets are ordered by updatedAt desc.
        val data = withContext(Dispatchers.IO) {
            WidgetData(repository.observeSummary().first(), repository.observeSheets().first().firstOrNull())
        }
        provideContent { Content(context, data) }
    }

    @Composable
    private fun Content(context: Context, data: WidgetData) {
        val sheet = data.lastSheet
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Surface).cornerRadius(20.dp).padding(12.dp)
                .semantics { contentDescription = context.getString(R.string.widget_open_description) }
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sheet == null) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
            } else {
                Text(
                    context.getString(R.string.widget_streak, data.summary.currentStreak),
                    style = TextStyle(color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    context.getString(R.string.widget_sets, data.summary.completedSets),
                    style = TextStyle(color = Muted, fontSize = 13.sp)
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(sheet.name, maxLines = 1, style = TextStyle(color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(6.dp))
                val start = actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_SHEET_ID, sheet.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                Text(
                    context.getString(R.string.widget_start),
                    modifier = GlanceModifier.fillMaxWidth().background(Accent).cornerRadius(16.dp).padding(vertical = 8.dp, horizontal = 12.dp)
                        .semantics { contentDescription = context.getString(R.string.widget_start_description, sheet.name) }
                        .clickable(start),
                    style = TextStyle(color = OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
