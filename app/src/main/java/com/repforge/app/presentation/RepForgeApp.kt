package com.repforge.app.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.RepForgeApplication
import com.repforge.app.presentation.home.HomeScreen
import com.repforge.app.presentation.home.HomeViewModel
import com.repforge.app.presentation.workout.WorkoutScreen
import com.repforge.app.presentation.workout.WorkoutViewModel
import com.repforge.app.presentation.progress.ProgressScreen
import com.repforge.app.presentation.progress.ProgressViewModel
import com.repforge.app.presentation.settings.SettingsScreen
import com.repforge.app.presentation.settings.SettingsViewModel
import com.repforge.app.data.AppPreferences
import androidx.lifecycle.ViewModelProvider

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Treino", Icons.Rounded.FitnessCenter),
    Progress("Evolução", Icons.Rounded.Timeline),
    Settings("Ajustes", Icons.Rounded.Settings)
}

@Composable
fun RepForgeApp() {
    var selected by remember { mutableStateOf(Destination.Home) }
    var activeSheet by remember { mutableStateOf<com.repforge.app.domain.model.TrainingSheet?>(null) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        if (activeSheet != null) {
            val application = LocalContext.current.applicationContext as RepForgeApplication
            val workoutViewModel: WorkoutViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    WorkoutViewModel(application.repository) as T
            })
            val sheet = activeSheet!!
            val preferences by application.settings.preferences.collectAsStateWithLifecycle(AppPreferences())
            WorkoutScreen(sheet.id, sheet.name, workoutViewModel, preferences.restSeconds) { activeSheet = null }
        } else if (selected == Destination.Home) {
            val application = LocalContext.current.applicationContext as RepForgeApplication
            val homeViewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(application.repository) as T
            })
            val state by homeViewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onCreateSheet = homeViewModel::createSheet,
                onStartSheet = { activeSheet = it },
                modifier = Modifier.padding(padding)
            )
        } else if (selected == Destination.Progress) {
            val application = LocalContext.current.applicationContext as RepForgeApplication
            val progressViewModel: ProgressViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ProgressViewModel(application.repository) as T
            })
            ProgressScreen(progressViewModel)
        } else {
            val application = LocalContext.current.applicationContext as RepForgeApplication
            val settingsViewModel: SettingsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(application.settings) as T
            })
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) scope.launch {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        OutputStreamWriter(output).use { it.write(application.backup.exportJson()) }
                    }
                }
            }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) scope.launch {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        InputStreamReader(input).use { application.backup.importJson(it.readText()) }
                    }
                }
            }
            SettingsScreen(
                settingsViewModel,
                onExport = { exportLauncher.launch("repforge-backup.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
            )
        }
    }
}
