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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
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
import com.repforge.app.domain.metrics.MuscleSets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.repforge.app.domain.metrics.ExerciseHistory
import com.repforge.app.domain.metrics.ProgressPoint
import com.repforge.app.domain.logging.formatDistanceKm
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import androidx.annotation.StringRes
import com.repforge.app.domain.metrics.ProgressRange
import androidx.compose.ui.platform.LocalConfiguration
import com.repforge.app.presentation.pluralCount
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import com.repforge.app.R
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.muscleLabel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/** "dd/MM" in pt-BR, "MM/dd" in en. */
@Composable
private fun dayFormat(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dd/MM"), locale) }
}

@Composable
private fun weekday(day: DayOfWeek) = stringArrayResource(R.array.weekdays_short)[day.value - 1]
// Per-app language can differ from Locale.getDefault(), so numbers follow the UI configuration.
@Composable
private fun kg(value: Double) = formatCount(value, LocalConfiguration.current.locales[0]) + " kg"
@Composable
private fun distance(km: Double) = formatDistanceKm(km, LocalConfiguration.current.locales[0])

/** Last logged value in the exercise's own unit; null when there is nothing meaningful (e.g. unloaded bodyweight). */
@Composable
private fun lastValue(ex: ExerciseHistory): String? = when (ex.kind) {
    KIND_TIMED -> ex.lastDurationSec?.let(::formatDuration)
    KIND_CARDIO -> ex.lastDistanceKm?.let { distance(it) } ?: ex.lastDurationSec?.let(::formatDuration)
    KIND_BODYWEIGHT -> ex.lastWeightKg.takeIf { it > 0 }?.let { kg(it) }
    else -> kg(ex.lastWeightKg)
}

@Composable
private fun rangeLabel(range: ProgressRange) = stringResource(
    when (range) {
        ProgressRange.Days30 -> R.string.range_30_days
        ProgressRange.Days90 -> R.string.range_90_days
        ProgressRange.All -> R.string.range_all
    }
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgressScreen(viewModel: ProgressViewModel, onOpenPro: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_progress)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.your_pace), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.pace_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.consistency), style = MaterialTheme.typography.titleMedium)
                        val c = state.consistency
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Stat(stringResource(R.string.streak), pluralCount(R.plurals.days_count, c.currentStreak))
                            Stat(stringResource(R.string.last_7_days), stringResource(R.string.x_of_7, c.daysLast7))
                            Stat(stringResource(R.string.days_trained), "${c.totalDays}")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.volume_per_day), style = MaterialTheme.typography.titleMedium)
                        val total = state.week.sumOf { it.volumeKg }
                        Text(stringResource(R.string.volume_summary, kg(total), pluralCount(R.plurals.sets_count, state.weekSets)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.week.isNotEmpty()) {
                            if (total == 0.0) Text(stringResource(R.string.log_first_workout), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            VolumeBarChart(state.week)
                        }
                    }
                }
            }
            item { HeatmapCard(state.heatmap) }
            item { BodyMapCard(state.bodyMap, state.muscleLocked, onOpenPro) }
            item { MuscleVolumeCard(state.muscleWeeks, state.muscleLocked, onOpenPro) }
            item { Text(stringResource(R.string.progress_by_exercise), style = MaterialTheme.typography.titleLarge) }
            if (state.exercises.isEmpty()) {
                item { Text(stringResource(R.string.exercises_appear_here), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.exercises, key = { "ex${it.exerciseId}" }) { ex ->
                val open = state.detail?.exercise?.exerciseId == ex.exerciseId
                Card {
                    Column(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable(onClickLabel = stringResource(if (open) R.string.collapse_chart else R.string.view_chart), role = Role.Button) { viewModel.toggleExercise(ex.exerciseId) }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(exerciseDisplayName(ex.name), style = MaterialTheme.typography.titleMedium)
                            Text(lastValue(ex)?.let { stringResource(R.string.sheet_last_load, ex.sheetName, it) } ?: ex.sheetName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(if (open) R.string.collapse else R.string.view_progress), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                        if (open) state.detail?.let { d ->
                            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProgressRange.entries.forEach { r ->
                                        FilterChip(selected = d.range == r, onClick = { viewModel.selectRange(r) }, label = { Text(if (r.pro) stringResource(R.string.range_pro, rangeLabel(r)) else rangeLabel(r)) })
                                    }
                                }
                                if (d.locked) ProUpsell(onOpenPro) else when (d.stats.kind) {
                                    KIND_TIMED -> ProgressChart(d.series, rangeLabel(d.range), { formatDuration(it.toInt()) }, R.string.chart_value_single, R.string.chart_value_range, R.string.value_range)
                                    KIND_CARDIO -> ProgressChart(d.series, rangeLabel(d.range), { distance(it) }, R.string.chart_value_single, R.string.chart_value_range, R.string.value_range)
                                    else -> ProgressChart(d.series, rangeLabel(d.range), { kg(it) }, R.string.chart_1rm_single, R.string.chart_1rm_range, R.string.est_1rm_range)
                                }
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    when (d.stats.kind) {
                                        KIND_TIMED -> Stat(stringResource(R.string.stat_longest_hold), formatDuration(d.stats.bestDurationSec))
                                        KIND_CARDIO -> Stat(stringResource(R.string.stat_longest_distance), distance(d.stats.bestDistanceKm))
                                        else -> if (d.stats.kind != KIND_BODYWEIGHT || d.stats.prKg > 0) {
                                            Stat(stringResource(R.string.stat_pr), kg(d.stats.prKg))
                                            Stat(stringResource(R.string.stat_best_1rm), kg(d.stats.bestOneRepMaxKg))
                                        }
                                    }
                                    Stat(stringResource(R.string.stat_total_sets), "${d.stats.totalSets}")
                                }
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.recent_workouts), style = MaterialTheme.typography.titleLarge) }
            if (state.recent.isEmpty()) {
                item { Text(stringResource(R.string.no_workouts_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.recent, key = { "day${it.date}" }) { w ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${weekday(w.date.dayOfWeek)}, ${w.date.format(dayFormat())}", style = MaterialTheme.typography.titleMedium)
                        Text(w.sheetNames.joinToString(", "))
                        Text(pluralCount(R.plurals.sets_count, w.sets) + " · " + kg(w.volumeKg), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProUpsell(
    onOpenPro: () -> Unit,
    title: String = stringResource(R.string.upsell_history_title),
    body: String = stringResource(R.string.upsell_history_body)
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenPro) { Text(stringResource(R.string.learn_pro)) }
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
    val summary = days.map { "${weekday(it.date.dayOfWeek)}: ${kg(it.volumeKg)}" }.joinToString(". ")
    val chartDescription = stringResource(R.string.volume_chart_description, summary)
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = chartDescription }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.peak, kg(days.maxOf { it.volumeKg })), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Line chart of [series]; [format] renders a value, the string ids are the accessibility summaries and the min-max caption. */
@Composable
private fun ProgressChart(
    series: List<ProgressPoint>, rangeName: String, format: @Composable (Double) -> String,
    @StringRes singleRes: Int, @StringRes rangeRes: Int, @StringRes captionRes: Int
) {
    if (series.isEmpty()) {
        Text(stringResource(R.string.no_records_in_period, rangeName), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val line = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outlineVariant
    val min = series.minOf { it.value }
    val max = series.maxOf { it.value }
    val dayFormat = dayFormat()
    val summary = if (series.size == 1) {
        stringResource(singleRes, series[0].date.format(dayFormat), format(series[0].value))
    } else {
        stringResource(
            rangeRes, series.first().date.format(dayFormat), series.last().date.format(dayFormat),
            format(series.first().value), format(series.last().value), format(min), format(max)
        )
    }
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(captionRes, format(min), format(max)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val pad = 8.dp.toPx()
            val w = size.width - 2 * pad
            val h = size.height - 2 * pad
            val firstDay = series.first().date.toEpochDay()
            val span = (series.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
            val spread = (max - min).takeIf { it > 0.0 }
            val points = series.map {
                val x = if (series.size == 1) size.width / 2f else pad + w * (it.date.toEpochDay() - firstDay) / span
                val y = if (spread == null) size.height / 2f else pad + h * (1f - ((it.value - min) / spread).toFloat())
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
        if (series.size == 1) Text(stringResource(R.string.log_more_for_line), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun sets(v: Double) = formatCount(v, LocalConfiguration.current.locales[0])

@Composable
private fun MuscleVolumeCard(weeks: List<List<MuscleSets>>, locked: Boolean, onOpenPro: () -> Unit) {
    val current = weeks.firstOrNull().orEmpty()
    val max = (weeks.flatten().maxOfOrNull { it.sets } ?: 0.0).coerceAtLeast(1.0)
    val labels = stringArrayResource(R.array.week_labels)
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.weekly_muscle_volume), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.weekly_muscle_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (current.isEmpty()) Text(stringResource(R.string.log_sets_for_muscle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            current.forEach { m ->
                val before = weeks.drop(1).map { w -> w.firstOrNull { it.muscle == m.muscle }?.sets ?: 0.0 }
                val muscleName = muscleLabel(m.muscle)
                val desc = stringResource(R.string.muscle_sets_value, muscleName, sets(m.sets)) +
                    if (before.isEmpty()) "" else stringResource(R.string.previous_weeks, before.map { sets(it) }.joinToString(", "))
                Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = desc }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(muscleName, style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.sets_value, sets(m.sets)), style = MaterialTheme.typography.labelLarge)
                    }
                    MuscleBar(m.sets / max)
                    if (before.isNotEmpty()) Text(
                        before.mapIndexed { i, v -> "${labels[i + 1]} ${sets(v)}" }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (locked) ProUpsell(onOpenPro, stringResource(R.string.upsell_muscle_title), stringResource(R.string.upsell_muscle_body))
        }
    }
}

@Composable
private fun MuscleBar(fraction: Double) {
    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxHeight().fillMaxWidth(fraction.toFloat().coerceIn(0.02f, 1f)).background(MaterialTheme.colorScheme.primary)) {}
    }
}
