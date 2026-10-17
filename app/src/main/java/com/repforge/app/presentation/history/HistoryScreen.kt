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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.metrics.HistoryItem
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.EmptyState
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SectionHeader
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.pluralCount
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onOpenSession: (Long) -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val dayFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LargeTitleHeader(stringResource(R.string.t5_history_title), action = {
                CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onBack)
            })
        }
        if (state.loaded && state.months.isEmpty()) item {
            EmptyState(Icons.Outlined.History, stringResource(R.string.t5_history_empty_title), stringResource(R.string.t5_history_empty_body))
        }
        state.months.forEach { (month, sessions) ->
            item(key = "m$month") {
                SectionHeader(monthLabel(month, locale), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
            }
            items(sessions, key = { "s${it.id}" }) { s ->
                SessionRow(s, dayFormat, locale, Modifier.padding(horizontal = 16.dp)) { onOpenSession(s.id) }
            }
        }
    }
    }
}

private fun monthLabel(month: YearMonth, locale: Locale) =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + month.year

@Composable
private fun SessionRow(s: HistoryItem, dayFormat: DateTimeFormatter, locale: Locale, modifier: Modifier, onClick: () -> Unit) {
    val date = Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormat)
    val meta = listOf(
        formatDuration((s.durationMs / 1000).toInt()),
        pluralCount(R.plurals.sets_count, s.sets),
        formatCount(s.volumeKg, locale) + " kg"
    ).joinToString(" · ")
    RfCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.sheetName ?: stringResource(R.string.t5_history_untitled), style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
