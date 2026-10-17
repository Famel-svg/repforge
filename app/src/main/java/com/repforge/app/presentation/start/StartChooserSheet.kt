package com.repforge.app.presentation.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateBodyWeight
import com.repforge.app.presentation.components.EmptyState
import com.repforge.app.presentation.components.GlyphSquare
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.pluralCount

/** Bottom sheet listing every sheet so the center Start button can launch one when nothing is planned for today. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StartChooserSheet(sheets: List<TrainingSheet>, onPick: (TrainingSheet) -> Unit, onOpenPlan: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.t2_choose_sheet), style = MaterialTheme.typography.titleLarge)
            if (sheets.isEmpty()) {
                EmptyState(
                    Icons.Outlined.FitnessCenter, stringResource(R.string.t2_no_sheets_title), stringResource(R.string.t2_no_sheets_body),
                    action = { SoftPillButton(stringResource(R.string.open_plan), null, onOpenPlan) }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sheets, key = { it.id }) { sheet ->
                        RfCard(Modifier.fillMaxWidth(), onClick = { onPick(sheet) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GlyphSquare(Icons.Outlined.FitnessCenter)
                                Column(Modifier.weight(1f)) {
                                    Text(sheet.name, style = MaterialTheme.typography.titleMedium)
                                    Text(pluralCount(R.plurals.exercises_count, sheet.exerciseCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.start_workout), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Optional: "Skip" starts the workout without a weight. */
@Composable
fun BodyWeightPrompt(onSave: (Double) -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val kg = parseWeightKg(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.body_weight_prompt_title)) },
        text = {
            OutlinedTextField(
                input, { input = it }, label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = { Button(enabled = validateBodyWeight(kg) == null, onClick = { onSave(kg!!) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.skip)) } }
    )
}
