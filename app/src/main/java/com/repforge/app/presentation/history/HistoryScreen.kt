package com.repforge.app.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.metrics.HistoryDay
import com.repforge.app.presentation.components.EmptyState
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SectionHeader
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.pluralCount
import com.repforge.app.presentation.progress.BackHeader
import com.repforge.app.presentation.progress.rememberListDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onOpenSession: (Long) -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val dayFormat = rememberListDateFormat()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackHeader(stringResource(R.string.t5_history_title), onBack) }
        if (state.loaded && state.months.isEmpty()) item {
            EmptyState(Icons.Outlined.History, stringResource(R.string.t5_history_empty_title), stringResource(R.string.t5_history_empty_body))
        }
        state.months.forEach { (month, days) ->
            item(key = "m$month") {
                SectionHeader(monthLabel(month, locale), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
            }
            items(days, key = { "d${it.date}" }) { d ->
                DayRow(d, dayFormat, locale, Modifier.padding(horizontal = 16.dp), onOpenSession)
            }
        }
    }
    }
}

private fun monthLabel(month: YearMonth, locale: Locale) =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + month.year

@Composable
private fun DayRow(d: HistoryDay, dayFormat: DateTimeFormatter, locale: Locale, modifier: Modifier, onOpenSession: (Long) -> Unit) {
    var expanded by rememberSaveable(d.date) { mutableStateOf(false) }
    val meta = listOfNotNull(
        d.durationMs?.let { formatDuration((it / 1000).toInt()) },
        pluralCount(R.plurals.sets_count, d.sets),
        formatCount(d.volumeKg, locale) + " kg"
    ).joinToString(" · ")
    val sessionId = d.sessionId
    RfCard(modifier.fillMaxWidth(), onClick = { if (sessionId != null) onOpenSession(sessionId) else expanded = !expanded }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(d.date.format(dayFormat), style = MaterialTheme.typography.titleMedium)
                    Text(d.sheetNames.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (sessionId != null) Icons.AutoMirrored.Outlined.KeyboardArrowRight else if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sessionId == null && expanded) d.exercises.forEach { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(exerciseDisplayName(e.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    val best = if (e.bestWeightKg > 0) " · " + stringResource(R.string.t5_history_best, formatCount(e.bestWeightKg, locale) + " kg × " + e.bestReps) else ""
                    Text(pluralCount(R.plurals.sets_count, e.sets) + best, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
