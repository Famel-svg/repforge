package com.repforge.app.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateSheetDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova ficha") },
        text = {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex.: Hipertrofia A") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Criar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}
