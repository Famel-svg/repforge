package com.repforge.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.presentation.theme.RepForgeTheme
import java.util.Locale

private fun fmt(v: Float) = String.format(Locale.getDefault(), if (v % 1f == 0f) "%.0f" else "%.1f", v)

/**
 * Area line chart: 2.5dp accent stroke, 35%→0% gradient fill, 3 dotted y-gridlines with tick labels,
 * evenly spread x [labels], dot + value on the last point and an optional dashed [goal] line.
 */
@Composable
fun AreaLineChart(
    points: List<Float>,
    goal: Float? = null,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val small = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant)
    val cd = if (points.isEmpty()) "" else stringResource(R.string.t1_chart_description, points.size, fmt(points.last()))
    Canvas(modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = cd }) {
        if (points.isEmpty()) return@Canvas
        val all = points + listOfNotNull(goal)
        var lo = all.min(); var hi = all.max()
        if (hi - lo < 1e-3f) { lo -= 1f; hi += 1f }
        val pad = (hi - lo) * 0.1f; lo -= pad; hi += pad
        val tickW = measurer.measure(fmt(hi), small).size.width + 8.dp.toPx()
        val labelH = if (labels.isEmpty()) 0f else 20.dp.toPx()
        val left = tickW; val top = 8.dp.toPx(); val right = size.width - 16.dp.toPx(); val bottom = size.height - labelH
        fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
        fun x(i: Int) = if (points.size == 1) (left + right) / 2 else left + i * (right - left) / (points.size - 1)

        val dots = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        for (k in 0..2) {
            val v = lo + (hi - lo) * k / 2f
            drawLine(cs.outlineVariant, Offset(left, y(v)), Offset(right, y(v)), 1.dp.toPx(), pathEffect = dots)
            val m = measurer.measure(fmt(v), small)
            drawText(m, topLeft = Offset(left - m.size.width - 6.dp.toPx(), y(v) - m.size.height / 2f))
        }
        labels.forEachIndexed { i, l ->
            val m = measurer.measure(l, small)
            val cx = if (labels.size == 1) left else left + i * (right - left) / (labels.size - 1)
            drawText(m, topLeft = Offset((cx - m.size.width / 2f).coerceIn(0f, size.width - m.size.width), bottom + 4.dp.toPx()))
        }
        if (goal != null) drawLine(
            cs.tertiary, Offset(left, y(goal)), Offset(right, y(goal)), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
        val line = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p)) else lineTo(x(i), y(p)) } }
        if (points.size > 1) {
            val area = Path().apply { addPath(line); lineTo(x(points.lastIndex), bottom); lineTo(x(0), bottom); close() }
            drawPath(area, Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.35f), cs.primary.copy(alpha = 0f)), startY = top, endY = bottom))
            drawPath(line, cs.primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        val lx = x(points.lastIndex); val ly = y(points.last())
        drawCircle(cs.primary, 4.5.dp.toPx(), Offset(lx, ly))
        val vm = measurer.measure(fmt(points.last()), TextStyle(color = cs.onSurface, fontSize = small.fontSize))
        drawText(vm, topLeft = Offset((lx - vm.size.width / 2f).coerceIn(left, size.width - vm.size.width), (ly - vm.size.height - 8.dp.toPx()).coerceAtLeast(0f)))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun AreaLineChartPreview() = RepForgeTheme(darkTheme = true) {
    AreaLineChart(listOf(82f, 81.4f, 81.9f, 80.6f, 80.1f, 79.5f), goal = 78f, labels = listOf("Jan", "Feb", "Mar"), modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AreaLineChartLightPreview() = RepForgeTheme(darkTheme = false) {
    AreaLineChart(listOf(5f, 6f, 4f, 8f), modifier = Modifier.padding(16.dp))
}
