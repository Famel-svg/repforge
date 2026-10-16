package com.repforge.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repforge.app.presentation.pluralCount
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.R
import com.repforge.app.domain.templates.StarterTemplates
import com.repforge.app.domain.templates.WorkoutTemplate

@Composable
fun CreateSheetDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onCreateFromTemplate: (WorkoutTemplate) -> Unit = {},
    initialTemplateId: String? = null
) {
    var templateId by rememberSaveable { mutableStateOf(initialTemplateId) }
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(StarterTemplates.firstOrNull { it.id == initialTemplateId }?.let { context.getString(it.sheetNameRes()) }.orEmpty()) }
    val template = StarterTemplates.firstOrNull { it.id == templateId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_sheet)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text(stringResource(R.string.sheet_name_placeholder)) },
                    singleLine = true
                )
                Text(stringResource(R.string.template), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.selectableGroup()) {
                    TemplateOption(stringResource(R.string.blank_sheet), null, selected = template == null) { templateId = null }
                    StarterTemplates.forEach { t ->
                        TemplateOption(
                            stringResource(t.labelRes()), pluralCount(R.plurals.exercises_count, t.exercises.size),
                            selected = t.id == templateId
                        ) { templateId = t.id; name = context.getString(t.sheetNameRes()) }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                if (template == null) onCreate(name) else onCreateFromTemplate(template.copy(sheetName = name.trim()))
            }) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun TemplateOption(label: String, detail: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(label)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Template display names live in string resources keyed by template id (stored exercise names stay catalog keys). */
@androidx.annotation.StringRes
private fun WorkoutTemplate.labelRes(): Int = when (id) {
    "full-body" -> R.string.template_full_body_label
    "abc-a" -> R.string.template_abc_a_label
    "abc-b" -> R.string.template_abc_b_label
    "abc-c" -> R.string.template_abc_c_label
    "upper" -> R.string.template_upper_label
    else -> R.string.template_lower_label
}

@androidx.annotation.StringRes
private fun WorkoutTemplate.sheetNameRes(): Int = when (id) {
    "full-body" -> R.string.template_full_body_sheet
    "abc-a" -> R.string.template_abc_a_sheet
    "abc-b" -> R.string.template_abc_b_sheet
    "abc-c" -> R.string.template_abc_c_sheet
    "upper" -> R.string.template_upper_sheet
    else -> R.string.template_lower_sheet
}
