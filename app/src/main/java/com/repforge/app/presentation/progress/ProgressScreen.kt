@file:OptIn(ExperimentalLayoutApi::class)

package com.repforge.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.domain.metrics.DayVolume
import com.repforge.app.domain.metrics.OneRepMaxPoint
import com.repforge.app.domain.metrics.ProgressRange
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

private val weekdayLabels = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
private val dayFormat = DateTimeFormatter.ofPattern("dd/MM")

private fun weekday(day: DayOfWeek) = weekdayLabels[day.value - 1]
private fun kg(value: Double) = if (value % 1.0 == 0.0) "%.0f kg".format(value) else "%.1f kg".format(value)
private fun rangeLabel(range: ProgressRange) = when (range) {
    ProgressRange.Days30 -> "30 dias"
    ProgressRange.Days90 -> "90 dias"
    ProgressRange.All -> "Tudo"
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgressScreen(viewModel: ProgressViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Evolução") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Seu ritmo", style = MaterialTheme.typography.headlineSmall)
                Text("Acompanhe consistência, não perfeição.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Consistência", style = MaterialTheme.typography.titleMedium)
                        val c = state.consistency
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat("Sequência", "${c.currentStreak} ${if (c.currentStreak == 1) "dia" else "dias"}")
                            Stat("Últimos 7 dias", "${c.daysLast7} de 7")
                            Stat("Dias treinados", "${c.totalDays}")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Volume por dia", style = MaterialTheme.typography.titleMedium)
                        val total = state.week.sumOf { it.volumeKg }
                        Text("${kg(total)} em ${state.weekSets} séries nos últimos 7 dias", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.week.isNotEmpty()) {
                            if (total == 0.0) Text("Registre seu primeiro treino para ver evolução.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            VolumeBarChart(state.week)
                        }
                    }
                }
            }
            item { Text("Evolução por exercício", style = MaterialTheme.typography.titleLarge) }
            if (state.exercises.isEmpty()) {
                item { Text("Os exercícios aparecem aqui depois do primeiro registro.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.exercises, key = { "ex${it.exerciseId}" }) { ex ->
                val open = state.detail?.exercise?.exerciseId == ex.exerciseId
                Card {
                    Column(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable(onClickLabel = if (open) "Recolher gráfico" else "Ver gráfico", role = Role.Button) { viewModel.toggleExercise(ex.exerciseId) }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium)
                            Text("${ex.sheetName} · última carga ${kg(ex.lastWeightKg)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (open) "Recolher" else "Ver evolução", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                        if (open) state.detail?.let { d ->
                            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProgressRange.entries.forEach { r ->
                                        FilterChip(selected = d.range == r, onClick = { viewModel.selectRange(r) }, label = { Text(rangeLabel(r)) })
                                    }
                                }
                                OneRepMaxChart(d.series, rangeLabel(d.range))
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Stat("Maior carga (PR)", kg(d.stats.prKg))
                                    Stat("Melhor 1RM est.", kg(d.stats.bestOneRepMaxKg))
                                    Stat("Total de séries", "${d.stats.totalSets}")
                                }
                            }
                        }
                    }
                }
            }
            item { Text("Treinos recentes", style = MaterialTheme.typography.titleLarge) }
            if (state.recent.isEmpty()) {
                item { Text("Nenhum treino registrado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.recent, key = { "day${it.date}" }) { w ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${weekday(w.date.dayOfWeek)}, ${w.date.format(dayFormat)}", style = MaterialTheme.typography.titleMedium)
                        Text(w.sheetNames.joinToString(", "))
                        Text("${w.sets} séries · ${kg(w.volumeKg)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VolumeBarChart(days: List<DayVolume>) {
    val max = days.maxOf { it.volumeKg }.coerceAtLeast(1.0)
    val bar = MaterialTheme.colorScheme.primary
    val stub = MaterialTheme.colorScheme.outline
    val summary = days.joinToString(". ") { "${weekday(it.date.dayOfWeek)}: ${kg(it.volumeKg)}" }
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Gráfico de barras do volume por dia. $summary" }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pico: ${kg(days.maxOf { it.volumeKg })}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / days.size
            val barWidth = slot * 0.6f
            days.forEachIndexed { i, d ->
                val h = if (d.volumeKg <= 0.0) 2.dp.toPx() else ((d.volumeKg / max).toFloat() * size.height).coerceAtLeast(4.dp.toPx())
                drawRect(
                    color = if (d.volumeKg <= 0.0) stub else bar,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - h),
                    size = Size(barWidth, h)
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { Text(weekday(it.date.dayOfWeek), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
    }
}

@Composable
private fun OneRepMaxChart(series: List<OneRepMaxPoint>, rangeName: String) {
    if (series.isEmpty()) {
        Text("Sem registros neste período ($rangeName).", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val line = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outlineVariant
    val min = series.minOf { it.oneRepMaxKg }
    val max = series.maxOf { it.oneRepMaxKg }
    val summary = if (series.size == 1) {
        "Um registro em ${series[0].date.format(dayFormat)}: 1RM estimado de ${kg(series[0].oneRepMaxKg)}"
    } else {
        "Evolução do 1RM estimado de ${series.first().date.format(dayFormat)} a ${series.last().date.format(dayFormat)}: " +
            "de ${kg(series.first().oneRepMaxKg)} para ${kg(series.last().oneRepMaxKg)}, mínimo ${kg(min)}, máximo ${kg(max)}"
    }
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("1RM estimado: ${kg(min)} a ${kg(max)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val pad = 8.dp.toPx()
            val w = size.width - 2 * pad
            val h = size.height - 2 * pad
            val firstDay = series.first().date.toEpochDay()
            val span = (series.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
            val spread = (max - min).takeIf { it > 0.0 }
            val points = series.map {
                val x = if (series.size == 1) size.width / 2f else pad + w * (it.date.toEpochDay() - firstDay) / span
                val y = if (spread == null) size.height / 2f else pad + h * (1f - ((it.oneRepMaxKg - min) / spread).toFloat())
                Offset(x, y)
            }
            drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
            }
            points.forEach { drawCircle(line, radius = 5.dp.toPx(), center = it) }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(series.first().date.format(dayFormat), style = MaterialTheme.typography.labelSmall)
            if (series.size > 1) Text(series.last().date.format(dayFormat), style = MaterialTheme.typography.labelSmall)
        }
        if (series.size == 1) Text("Registre mais treinos para ver a linha de evolução.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
