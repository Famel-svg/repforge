package com.repforge.app.presentation.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.domain.progression.ProgressionSuggestion

/** One-line progression suggestion. Free users see a "Pro" teaser; a null [suggestion] hides it for Pro users. */
@Composable
fun ProgressionHint(
    suggestion: ProgressionSuggestion?,
    isPro: Boolean,
    onApply: (reps: Int, weightKg: Double) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    if (!isPro) {
        Card(modifier.fillMaxWidth().clickable(onClickLabel = "Conhecer o Pro", role = Role.Button, onClick = onUpgrade), colors = colors) {
            Text("Sugestão de carga · Pro", Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    if (suggestion == null) return
    val w = suggestion.weightKg.let { if (it % 1.0 == 0.0) "%.0f".format(it) else "%.1f".format(it).replace('.', ',') }
    Card(modifier.fillMaxWidth(), colors = colors) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sugestão: $w kg × ${suggestion.reps} · ${suggestion.reason}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { onApply(suggestion.reps, suggestion.weightKg) }) { Text("Aplicar") }
        }
    }
}
