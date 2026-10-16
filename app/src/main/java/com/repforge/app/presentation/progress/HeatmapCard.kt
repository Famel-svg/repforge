package com.repforge.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.repforge.app.R
import com.repforge.app.domain.metrics.Heatmap
import com.repforge.app.domain.metrics.HeatmapCell
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle

@Composable
private fun heatColor(level: Int): Color =
    if (level == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f * level)

@Composable
fun HeatmapCard(heatmap: Heatmap) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    var selected by remember { mutableStateOf<HeatmapCell?>(null) }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val colors = (0..4).map { heatColor(it) }
    val description = pluralStringResource(R.plurals.heatmap_description, heatmap.trainedDays, heatmap.trainedDays)
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.heatmap_title), style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val cols = heatmap.weeks.size.coerceAtLeast(1)
            Canvas(
                Modifier.fillMaxWidth().height(100.dp).semantics { contentDescription = description }
                    .pointerInput(heatmap) {
                        detectTapGestures { p ->
                            val cell = size.width / cols.toFloat()
                            val top = 14.dp.toPx()
                            val w = (p.x / cell).toInt()
                            val r = ((p.y - top) / cell).toInt()
                            selected = if (p.y >= top) heatmap.weeks.getOrNull(w)?.getOrNull(r) else null
                        }
                    }
            ) {
                val cell = size.width / cols
                val gap = cell * 0.18f
                val top = 14.dp.toPx()
                var lastLabel = -3
                heatmap.weeks.forEachIndexed { w, days ->
                    days.firstOrNull { it?.date?.dayOfMonth == 1 }?.let { first ->
                        if (w - lastLabel >= 3) {
                            lastLabel = w
                            drawText(measurer, first.date.month.getDisplayName(DateTextStyle.SHORT, locale), Offset(w * cell, 0f), labelStyle)
                        }
                    }
                    days.forEachIndexed { r, c ->
                        if (c != null) drawRoundRect(
                            colors[c.level], Offset(w * cell, top + r * cell), Size(cell - gap, cell - gap), CornerRadius(cell * 0.2f)
                        )
                    }
                }
                selected?.let { s ->
                    heatmap.weeks.forEachIndexed { w, days ->
                        val r = days.indexOf(s)
                        if (r >= 0) drawRoundRect(
                            colors[4], Offset(w * cell, top + r * cell), Size(cell - gap, cell - gap), CornerRadius(cell * 0.2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                        )
                    }
                }
            }
            val s = selected
            Text(
                when {
                    s == null -> stringResource(R.string.heatmap_tap_hint)
                    s.sets == 0 -> stringResource(R.string.heatmap_cell_empty, s.date.format(dateFormat))
                    else -> stringResource(R.string.heatmap_cell, s.date.format(dateFormat), pluralStringResource(R.plurals.sets_count, s.sets, s.sets))
                },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall)
                colors.forEach { Canvas(Modifier.size(10.dp)) { drawRoundRect(it, cornerRadius = CornerRadius(2.dp.toPx())) } }
                Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
