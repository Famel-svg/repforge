package com.repforge.app.presentation.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDistanceKm
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.metrics.SessionRecord
import com.repforge.app.domain.metrics.SessionSummary
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SectionHeader
import com.repforge.app.presentation.components.Tag
import com.repforge.app.presentation.components.TagTone
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.formatCount
import com.repforge.app.presentation.muscleLabel
import com.repforge.app.presentation.setsText
import com.repforge.app.presentation.workout.rememberReducedMotion

/** Full-screen celebration after a workout: scale-in check, stat tiles, new records and a filled "Done". */
@Composable
fun SessionSummaryScreen(
    viewModel: SessionSummaryViewModel,
    onDone: () -> Unit,
    // Route `summary/{sessionId}?history={bool}` wins when set; the parameter is the fallback for callers that pass it directly.
    fromHistory: Boolean = false
) {
    val history = fromHistory || ((LocalViewModelStoreOwner.current as? NavBackStackEntry)?.arguments?.getBoolean("history", false) == true)
    val summary by viewModel.state.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val s = summary
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (s == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else Summary(s, history, header)
        }
        Button(
            onClick = onDone, shape = CircleShape,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 52.dp)
        ) { Text(stringResource(if (history) R.string.t4_summary_back else R.string.summary_done), style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun Summary(s: SessionSummary, history: Boolean, header: Pair<String?, Long>?) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!history) CheckBadge()
        Text(
            if (history) listOfNotNull(header?.first, header?.second?.let { java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(java.util.Date(it)) })
                .joinToString(" · ").ifEmpty { stringResource(R.string.t4_summary_back) }
            else stringResource(R.string.summary_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.summary_duration), formatDuration((s.durationMs / 1000).toInt()), Modifier.weight(1f))
            StatTile(stringResource(R.string.summary_sets), s.sets.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.summary_volume), formatCount(s.volumeKg, locale) + " kg", Modifier.weight(1f))
        }
        if (s.sets == 0) Text(stringResource(R.string.summary_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(if (history) R.string.t4_summary_records_neutral else R.string.summary_records))
                RfCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (s.records.isEmpty()) Text(stringResource(if (history) R.string.t4_summary_no_records_history else R.string.summary_no_records), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        s.records.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Tag(stringResource(R.string.t4_summary_pr_badge), TagTone.Accent)
                                Text(exerciseDisplayName(r.exerciseName), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text(recordText(r), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.summary_exercises))
                RfCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.exercises.forEach { (_, name) -> Text(exerciseDisplayName(name), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
            if (s.muscles.isNotEmpty()) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.summary_muscles))
                RfCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.muscles.forEach { m ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(muscleLabel(m.muscle), style = MaterialTheme.typography.bodyLarge)
                                Text(setsText(m.sets), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Accent check in a soft circle that scales in; snaps when the system animator scale is 0. */
@Composable
private fun CheckBadge() {
    val reduced = rememberReducedMotion()
    val scale = remember { Animatable(if (reduced) 1f else 0.4f) }
    LaunchedEffect(Unit) { if (!reduced) scale.animateTo(1f, tween(350)) }
    Box(
        Modifier.size(112.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    RfCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

private fun recordText(r: SessionRecord) = when (r.kind) {
    KIND_TIMED -> formatDuration(r.value.toInt())
    KIND_CARDIO -> formatDistanceKm(r.value)
    else -> formatKg(r.value)
}
