package com.repforge.app.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import com.repforge.app.data.ProStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun SettingsScreen(viewModel: SettingsViewModel, pro: ProStore, onOpenBodyWeight: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    val price by pro.price.collectAsStateWithLifecycle()
    val proError by pro.error.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val resolver = context.contentResolver
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup { json ->
            (resolver.openOutputStream(uri) ?: throw IOException("destino indisponível")).use { it.write(json.toByteArray()) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup {
            resolver.readTextCapped(uri)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importCsv {
            resolver.readTextCapped(uri)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("RepForge Pro", style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isPro) {
                        Text("Pro ativo", style = MaterialTheme.typography.titleMedium)
                        Text("Obrigado por apoiar o RepForge! Histórico de 90 dias e completo liberado, para sempre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Desbloqueio vitalício${price?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                        Text("Pagamento único, sem assinatura. Libera os gráficos de 1RM de 90 dias e de todo o histórico.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        proError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { activity?.let(pro::purchase) }, enabled = activity != null) { Text(price?.let { "Comprar por $it" } ?: "Comprar Pro") }
                            OutlinedButton(onClick = pro::restore) { Text("Restaurar compra") }
                        }
                    }
                }
            }
            Text("Treino", style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenBodyWeight) { Text("Peso corporal") }
                    Text("Descanso padrão", style = MaterialTheme.typography.titleMedium)
                    Text("${state.restSeconds} segundos entre séries")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120, 180).forEach { seconds ->
                            FilterChip(selected = seconds == state.restSeconds, onClick = { viewModel.setRestSeconds(seconds) }, label = { Text("${seconds}s") })
                        }
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().toggleable(state.hapticsEnabled, role = Role.Switch, onValueChange = { viewModel.setHapticsEnabled(it) }),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Feedback háptico", style = MaterialTheme.typography.titleMedium)
                            Text("Vibrar ao registrar uma série", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.hapticsEnabled, onCheckedChange = null)
                    }
                }
            }
            Text("Backup", style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Importar adiciona as fichas do arquivo às atuais; nada é apagado. Backups do RepForge 1.0 (Expo) também são aceitos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("repforge-backup-${LocalDate.now()}.json") }) { Text("Exportar backup") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Importar backup") }
                        OutlinedButton(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }) { Text("Importar do Strong/Hevy (CSV)") }
                    }
                }
            }
            Text("RepForge $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Seus treinos ficam no aparelho e funcionam sem conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val MAX_IMPORT_BYTES = 20L * 1024 * 1024

/** Reads a picked file as text, refusing anything over 20 MB before loading it. */
private fun ContentResolver.readTextCapped(uri: Uri): String {
    val size = query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
    if (size != null && size > MAX_IMPORT_BYTES) throw IOException("arquivo maior que 20 MB")
    return (openInputStream(uri) ?: throw IOException("arquivo indisponível")).use { it.readBytes().decodeToString() }
}
