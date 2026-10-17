package com.repforge.app.presentation.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.formatWeightInput
import com.repforge.app.domain.progression.ProgressionKind
import com.repforge.app.domain.progression.ProgressionSuggestion

/** One-line progression suggestion. Free users see a "Pro" teaser; a null [suggestion] hides it for Pro users. */
@Composable
fun ProgressionHint(
    suggestion: ProgressionSuggestion?,
    isPro: Boolean,
    onApply: (reps: Int, weightKg: Double, durationSec: Int?) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isPro) {
        HintPill(modifier.clickable(onClickLabel = stringResource(R.string.learn_pro), role = Role.Button, onClick = onUpgrade)) {
            Text(stringResource(R.string.suggestion_pro_teaser), Modifier.weight(1f).padding(vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    if (suggestion == null) return
    val w = formatWeightInput(suggestion.weightKg)
    val reason = when (suggestion.kind) {
        ProgressionKind.Deload -> if (suggestion.topReps > 0) stringResource(R.string.progression_deload_failed, suggestion.topReps) else stringResource(R.string.progression_deload, 10)
        ProgressionKind.RaiseLoad -> stringResource(R.string.progression_raise, suggestion.topReps)
        ProgressionKind.KeepLoad -> if (suggestion.weightKg == 0.0) stringResource(R.string.progression_keep_bw, suggestion.reps) else stringResource(R.string.progression_keep, suggestion.reps, w, suggestion.reps)
        ProgressionKind.AddRep -> if (suggestion.weightKg == 0.0) stringResource(R.string.progression_add_rep_bw, suggestion.reps) else stringResource(R.string.progression_add_rep, w, suggestion.reps)
        ProgressionKind.ExtraLoad -> stringResource(R.string.progression_extra_load, suggestion.topReps, w, suggestion.reps)
        ProgressionKind.AddTime -> stringResource(R.string.progression_time_add, formatDuration(suggestion.durationSec ?: 0))
        ProgressionKind.KeepTime -> stringResource(R.string.progression_time_keep, formatDuration(suggestion.durationSec ?: 0))
    }
    val line = when {
        suggestion.durationSec != null -> stringResource(R.string.suggestion_line_time, formatDuration(suggestion.durationSec), reason)
        suggestion.weightKg == 0.0 -> stringResource(R.string.suggestion_line_reps, suggestion.reps, reason)
        else -> stringResource(R.string.suggestion_line, w, suggestion.reps, reason)
    }
    HintPill(modifier) {
        Text(line, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { onApply(suggestion.reps, suggestion.weightKg, suggestion.durationSec) }) { Text(stringResource(R.string.apply)) }
    }
}

/** Soft accent pill with a bulb icon; [content] is laid out in a row after the icon. */
@Composable
private fun HintPill(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp))
            content()
        }
    }
}
