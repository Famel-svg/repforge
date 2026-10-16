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
    var name by rememberSaveable { mutableStateOf(StarterTemplates.firstOrNull { it.id == initialTemplateId }?.sheetName.orEmpty()) }
    val template = StarterTemplates.firstOrNull { it.id == templateId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova ficha") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex.: Hipertrofia A") },
                    singleLine = true
                )
                Text("Modelo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.selectableGroup()) {
                    TemplateOption("Ficha em branco", null, selected = template == null) { templateId = null }
                    StarterTemplates.forEach { t ->
                        TemplateOption(t.label, "${t.exercises.size} exercícios", selected = t.id == templateId) { templateId = t.id; name = t.sheetName }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                if (template == null) onCreate(name) else onCreateFromTemplate(template.copy(sheetName = name.trim()))
            }) { Text("Criar") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
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
