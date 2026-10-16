package com.repforge.app.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDistanceKm
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.metrics.SessionRecord
import com.repforge.app.domain.metrics.SessionSummary
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.muscleLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(viewModel: SessionSummaryViewModel, onDone: () -> Unit) {
    val summary by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.summary_title)) }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.summary_done))
                }
            }
        }
    ) { padding ->
        val s = summary
        if (s == null) Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        } else Summary(s, Modifier.padding(padding))
    }
}

@Composable
private fun Summary(s: SessionSummary, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.summary_duration), formatDuration((s.durationMs / 1000).toInt()))
                Stat(stringResource(R.string.summary_sets), s.sets.toString())
                Stat(stringResource(R.string.summary_volume), formatCount(kotlin.math.round(s.volumeKg), locale) + " kg")
            }
        }
        if (s.sets == 0) Text(stringResource(R.string.summary_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            Section(stringResource(R.string.summary_records)) {
                if (s.records.isEmpty()) Text(stringResource(R.string.summary_no_records), color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.records.forEach { Text("${exerciseDisplayName(it.exerciseName)} · ${recordText(it)}") }
            }
            Section(stringResource(R.string.summary_exercises)) { s.exercises.forEach { (_, name) -> Text(exerciseDisplayName(name)) } }
            if (s.muscles.isNotEmpty()) Section(stringResource(R.string.summary_muscles)) {
                s.muscles.forEach { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(muscleLabel(m.muscle))
                        Text(com.repforge.app.presentation.setsText(m.sets), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun recordText(r: SessionRecord) = when (r.kind) {
    KIND_TIMED -> formatDuration(r.value.toInt())
    KIND_CARDIO -> formatDistanceKm(r.value)
    else -> formatKg(r.value)
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}
