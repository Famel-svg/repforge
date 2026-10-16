@file:OptIn(ExperimentalLayoutApi::class)

package com.repforge.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.metrics.BODY_MUSCLES
import com.repforge.app.domain.metrics.BodyRange
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.muscleLabel

/** Stylized silhouette in a 100x200 box; each muscle is a few simple rounded shapes (mirrored pairs where symmetric). */
private class Region(val muscle: String, val path: Path)

private fun region(muscle: String, vararg rects: FloatArray, mirror: Boolean = true, oval: Boolean = false): Region {
    val p = Path()
    fun add(l: Float, t: Float, r: Float, b: Float) =
        if (oval) p.addOval(Rect(l, t, r, b)) else p.addRoundRect(RoundRect(l, t, r, b, CornerRadius(3f, 3f)))
    rects.forEach { (l, t, r, b) ->
        add(l, t, r, b)
        if (mirror) add(100 - r, t, 100 - l, b)
    }
    return Region(muscle, p)
}

private fun silhouette() = Path().apply {
    addOval(Rect(39f, 1f, 61f, 25f))
    addRoundRect(RoundRect(44f, 24f, 56f, 32f, CornerRadius(2f, 2f)))
    addRoundRect(RoundRect(29f, 30f, 71f, 100f, CornerRadius(10f, 10f)))
    listOf(floatArrayOf(17f, 33f, 30f, 74f), floatArrayOf(13f, 73f, 26f, 112f), floatArrayOf(31f, 98f, 50f, 150f), floatArrayOf(31f, 149f, 48f, 192f)).forEach { (l, t, r, b) ->
        addRoundRect(RoundRect(l, t, r, b, CornerRadius(6f, 6f)))
        addRoundRect(RoundRect(100 - r, t, 100 - l, b, CornerRadius(6f, 6f)))
    }
}

private val SHOULDER = floatArrayOf(20f, 31f, 33f, 46f)
private val ARM = floatArrayOf(19f, 47f, 28f, 71f)
private val FOREARM = floatArrayOf(15f, 76f, 24f, 108f)
private val CALF = floatArrayOf(33f, 153f, 46f, 188f)

private val FRONT = listOf(
    region("Ombros", SHOULDER, oval = true), region("Peito", floatArrayOf(34f, 36f, 49f, 55f)),
    region("Bíceps", ARM), region("Antebraço", FOREARM), region("Abdômen", floatArrayOf(40f, 58f, 60f, 90f), mirror = false),
    region("Quadríceps", floatArrayOf(32f, 102f, 48f, 146f)), region("Panturrilha", CALF)
)
private val BACK = listOf(
    region("Trapézio", floatArrayOf(39f, 27f, 61f, 46f), mirror = false), region("Ombros", SHOULDER, oval = true),
    region("Costas", floatArrayOf(33f, 47f, 67f, 72f), mirror = false), region("Tríceps", ARM), region("Antebraço", FOREARM),
    region("Lombar", floatArrayOf(39f, 74f, 61f, 90f), mirror = false), region("Glúteos", floatArrayOf(32f, 92f, 49f, 112f)),
    region("Posterior de coxa", floatArrayOf(32f, 114f, 48f, 148f)), region("Panturrilha", CALF)
)

@Composable
private fun Figure(regions: List<Region>, totals: Map<String, Double>, max: Double, modifier: Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outline
    val body = remember { silhouette() }
    Canvas(modifier.aspectRatio(0.5f)) {
        val s = size.width / 100f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawPath(body, base)
            regions.forEach { r ->
                val v = totals[r.muscle] ?: 0.0
                if (v > 0) drawPath(r.path, fill.copy(alpha = (0.25 + 0.75 * v / max).toFloat().coerceIn(0.25f, 1f)))
                drawPath(r.path, line, style = Stroke(0.6f))
            }
        }
    }
}

@Composable
fun BodyMapCard(bodyMap: Map<BodyRange, Map<String, Double>>, locked: Boolean, onOpenPro: () -> Unit) {
    var range by rememberSaveable { mutableStateOf(BodyRange.Week) }
    val rangeLocked = range.pro && locked
    val totals = if (rangeLocked) emptyMap() else bodyMap[range].orEmpty()
    val max = (totals.values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val locale = LocalConfiguration.current.locales[0]
    fun n(v: Double) = formatCount(v, locale)
    val ranked = BODY_MUSCLES.filter { (totals[it] ?: 0.0) > 0 }.sortedByDescending { totals[it] }
    val untrained = BODY_MUSCLES.filter { (totals[it] ?: 0.0) <= 0 }
    val labels = BODY_MUSCLES.map { it to muscleLabel(it) }.toMap()
    val summary = ranked.joinToString(", ") { "${labels.getValue(it)} ${n(totals.getValue(it))}" }
        .ifEmpty { stringResource(R.string.bodymap_none) }
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.bodymap_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.bodymap_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyRange.entries.forEach { r ->
                    val name = stringResource(
                        when (r) {
                            BodyRange.Week -> R.string.bodymap_week
                            BodyRange.Month -> R.string.bodymap_month
                            BodyRange.All -> R.string.range_all
                        }
                    )
                    FilterChip(selected = range == r, onClick = { range = r }, label = { Text(if (r.pro) stringResource(R.string.range_pro, name) else name) })
                }
            }
            if (rangeLocked) {
                ProUpsell(onOpenPro)
            } else {
                Row(
                    Modifier.fillMaxWidth().semantics { contentDescription = summary },
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top
                ) {
                    listOf(R.string.bodymap_front to FRONT, R.string.bodymap_back to BACK).forEach { (title, regions) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Figure(regions, totals, max, Modifier.fillMaxWidth(0.8f))
                            Text(stringResource(title), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (ranked.isEmpty()) Text(stringResource(R.string.bodymap_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                ranked.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(labels.getValue(it), style = MaterialTheme.typography.labelLarge)
                        Text(com.repforge.app.presentation.setsText(totals.getValue(it)), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(stringResource(R.string.bodymap_untrained), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (untrained.isEmpty()) stringResource(R.string.bodymap_all_trained) else untrained.joinToString(", ") { labels.getValue(it) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
