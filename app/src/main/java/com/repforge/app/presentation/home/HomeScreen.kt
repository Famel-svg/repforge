package com.repforge.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.presentation.components.AreaLineChart
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.GlyphSquare
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.components.Tag
import com.repforge.app.presentation.components.TagTone
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.pluralCount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    weightGoalKg: Double?,
    weeklyTarget: Int,
    onStartSheet: (TrainingSheet) -> Unit,
    onResumeSheet: (TrainingSheet) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        LargeTitleHeader(
            stringResource(R.string.app_name),
            today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale)).replaceFirstChar { it.titlecase(locale) },
            action = { CircleIconButton(Icons.Outlined.Settings, stringResource(R.string.nav_settings), onOpenSettings) }
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WeekCard(state, today, locale, onStartSheet, onResumeSheet, onShiftWeek, onOpenPlan)
            BodyWeightCard(state, weightGoalKg, locale, onOpenBody)
            StreakCard(state, weeklyTarget, today, onOpenHistory)
        }
    }
}

@Composable
private fun WeekCard(
    state: HomeUiState, today: LocalDate, locale: Locale,
    onStart: (TrainingSheet) -> Unit, onResume: (TrainingSheet) -> Unit, onShiftWeek: (Int) -> Unit, onOpenPlan: () -> Unit
) {
    RfCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onShiftWeek(-1) }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.t2_prev_week)) }
                Text(stringResource(R.string.t2_this_week), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                IconButton(onClick = { onShiftWeek(1) }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.t2_next_week)) }
            }
            Row(Modifier.fillMaxWidth()) { state.week.forEach { DayColumn(it, today, locale, Modifier.weight(1f)) } }
            TodayRow(state, onStart, onResume, onOpenPlan)
        }
    }
}

@Composable
private fun DayColumn(day: WeekDay, today: LocalDate, locale: Locale, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val isToday = day.date == today
    val name = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
    val full = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val status = when {
        day.trained -> stringResource(R.string.t2_day_trained, full)
        day.planned -> stringResource(R.string.t2_day_planned, full)
        else -> full
    }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = status }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(name.uppercase(locale), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Box(Modifier.size(34.dp).background(if (isToday) cs.primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = if (isToday) cs.onPrimary else cs.onSurface)
        }
        val dot = when { day.trained -> cs.primary; day.planned -> cs.outline; else -> androidx.compose.ui.graphics.Color.Transparent }
        Box(Modifier.size(6.dp).background(dot, CircleShape))
    }
}

@Composable
private fun TodayRow(state: HomeUiState, onStart: (TrainingSheet) -> Unit, onResume: (TrainingSheet) -> Unit, onOpenPlan: () -> Unit) {
    val today = state.today ?: return
    val resume = state.resume
    val sheet = resume ?: today.sheet
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlyphSquare(Icons.Outlined.FitnessCenter)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.t2_today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    sheet?.name ?: stringResource(if (today.configured) R.string.t2_rest else R.string.set_plan_title),
                    style = MaterialTheme.typography.titleMedium, maxLines = 2
                )
            }
            when {
                resume != null -> SoftPillButton(stringResource(R.string.t2_tab_resume), null, { onResume(resume) })
                today.sheet != null -> SoftPillButton(stringResource(R.string.t2_tab_start), null, { onStart(today.sheet) })
                !today.configured -> SoftPillButton(stringResource(R.string.open_plan), null, onOpenPlan)
            }
        }
    }
}

@Composable
private fun BodyWeightCard(state: HomeUiState, goalKg: Double?, locale: Locale, onOpenBody: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val weights = state.bodyWeights.takeLast(30)
    RfCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.t2_body_weight), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                if (goalKg != null) Tag(stringResource(R.string.t2_goal_chip, formatCount(goalKg, locale)), TagTone.Goal)
                Spacer(Modifier.weight(1f))
                SoftPillButton(stringResource(R.string.t2_log_weight), Icons.Outlined.Add, onOpenBody)
            }
            if (weights.isEmpty()) {
                Text(stringResource(R.string.t2_no_weight), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            } else {
                val last = weights.last().weightKg
                val delta = if (weights.size > 1) last - weights[weights.size - 2].weightKg else 0.0
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatCount(last, locale), style = MaterialTheme.typography.displayMedium)
                    Text("kg", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    if (delta != 0.0) {
                        Icon(if (delta > 0) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward, null, tint = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp).size(16.dp))
                        Text(formatCount(kotlin.math.abs(delta), locale), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                if (goalKg != null) Text(stringResource(R.string.t2_goal_line, formatCount(goalKg, locale)), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                val zone = java.time.ZoneId.systemDefault()
                val month = com.repforge.app.presentation.progress.dayFormat()
                fun label(i: Int) = java.time.Instant.ofEpochMilli(weights[i].recordedAt).atZone(zone).format(month)
                AreaLineChart(
                    points = weights.map { it.weightKg.toFloat() }, goal = goalKg?.toFloat(),
                    labels = if (weights.size > 1) listOf(label(0), label(weights.lastIndex)) else listOf(label(0))
                )
            }
        }
    }
}

@Composable
private fun StreakCard(state: HomeUiState, target: Int, today: LocalDate, onOpenHistory: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val monday = remember(today) { mondayOf(today) }
    val streak = streakWeeks(state.weeklyCounts, target, monday)
    val thisWeek = state.weeklyCounts[monday] ?: 0
    RfCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.LocalFireDepartment, null, tint = cs.secondary, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(pluralCount(R.plurals.t2_streak_weeks, streak), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.t2_week_progress, thisWeek, target, state.weeklyCounts.values.sum()),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant
                )
            }
            CircleIconButton(Icons.Outlined.CalendarMonth, stringResource(R.string.t2_history), onOpenHistory)
        }
    }
}
