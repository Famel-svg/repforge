package com.repforge.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.repforge.app.presentation.components.RfCard
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
import com.repforge.app.presentation.pluralCount
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
    if (level == 0) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f * level)

@Composable
fun HeatmapCard(heatmap: Heatmap) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = rememberListDateFormat()
    var selected by remember { mutableStateOf<HeatmapCell?>(null) }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val colors = (0..4).map { heatColor(it) }
    val description = pluralCount(R.plurals.heatmap_description, heatmap.trainedDays)
    val accent = MaterialTheme.colorScheme.primary
    val today = heatmap.weeks.lastOrNull()?.lastOrNull { it != null }
    RfCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.heatmap_title), style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val cols = heatmap.weeks.size.coerceAtLeast(1)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Square cells capped at the legend's size so the canvas is exactly as tall as the grid.
                val cellDp = minOf(maxWidth / cols, 12.dp)
                val topDp = 14.dp
                Canvas(
                    Modifier.fillMaxWidth().height(topDp + cellDp * 7).semantics { contentDescription = description }
                        .pointerInput(heatmap) {
                            detectTapGestures { p ->
                                val cell = cellDp.toPx()
                                val top = topDp.toPx()
                                val w = (p.x / cell).toInt()
                                val r = ((p.y - top) / cell).toInt()
                                selected = if (p.y >= top) heatmap.weeks.getOrNull(w)?.getOrNull(r) else null
                            }
                        }
                ) {
                    val cell = cellDp.toPx()
                    val gap = 2.dp.toPx()
                    val radius = CornerRadius((cell - gap) * 0.3f) // legend proportions: 3dp on 10dp
                    val top = topDp.toPx()
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
                                colors[c.level], Offset(w * cell, top + r * cell), Size(cell - gap, cell - gap), radius
                            )
                        }
                    }
                    selected?.let { s ->
                        heatmap.weeks.forEachIndexed { w, days ->
                            val r = days.indexOf(s)
                            if (r >= 0) drawRoundRect(
                                colors[4], Offset(w * cell, top + r * cell), Size(cell - gap, cell - gap), radius,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                            )
                        }
                    }
                }
            }
            val s = selected
            Text(
                when {
                    s == null -> stringResource(R.string.heatmap_tap_hint)
                    s.sets == 0 -> stringResource(R.string.heatmap_cell_empty, s.date.format(dateFormat))
                    else -> stringResource(R.string.heatmap_cell, s.date.format(dateFormat), pluralCount(R.plurals.sets_count, s.sets))
                },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                colors.forEach { Canvas(Modifier.size(10.dp)) { drawRoundRect(it, cornerRadius = CornerRadius(3.dp.toPx())) } }
                Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
