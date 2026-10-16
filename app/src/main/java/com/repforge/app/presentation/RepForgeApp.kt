package com.repforge.app.presentation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.repforge.app.presentation.onboarding.OnboardingScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.repforge.app.RepForgeApplication
import com.repforge.app.data.AppPreferences
import com.repforge.app.presentation.home.HomeScreen
import com.repforge.app.presentation.home.HomeViewModel
import com.repforge.app.presentation.progress.ProgressScreen
import com.repforge.app.presentation.progress.ProgressViewModel
import com.repforge.app.presentation.search.ExerciseSearchScreen
import com.repforge.app.presentation.search.ExerciseSearchViewModel
import com.repforge.app.presentation.settings.SettingsScreen
import com.repforge.app.presentation.settings.SettingsViewModel
import com.repforge.app.presentation.workout.WorkoutScreen
import com.repforge.app.presentation.workout.WorkoutViewModel

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Treino", Icons.Rounded.FitnessCenter),
    Progress("progress", "Evolução", Icons.Rounded.Timeline),
    Settings("settings", "Ajustes", Icons.Rounded.Settings)
}

private const val SHEET_ID = "sheetId"
private val sheetArguments = listOf(navArgument(SHEET_ID) { type = NavType.LongType })

@Composable
fun RepForgeApp() {
    val application = LocalContext.current.applicationContext as RepForgeApplication
    val navController = rememberNavController()
    // null until DataStore emits, so the start destination is never wrong for a frame.
    val onboardingDone by application.settings.onboardingDone.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    if (onboardingDone == null) return
    val startRoute = remember { if (onboardingDone == true) Destination.Home.route else "onboarding" } // fixed at first value
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            // Sheet and search are full-screen flows; the tab bar only belongs to top-level destinations.
            if (Destination.entries.any { it.route == currentRoute }) NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = startRoute, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("onboarding") {
                OnboardingScreen(onFinish = {
                    scope.launch { application.settings.setOnboardingDone() }
                    navController.navigate(Destination.Home.route) { popUpTo("onboarding") { inclusive = true } }
                })
            }
            composable(Destination.Home.route) {
                val homeViewModel = viewModel { HomeViewModel(application.repository) }
                val state by homeViewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onCreateSheet = homeViewModel::createSheet,
                    onStartSheet = { navController.navigate("sheet/${it.id}") },
                    onCreateSheetFromTemplate = homeViewModel::createSheetFromTemplate,
                    onDuplicateSheet = homeViewModel::duplicateSheet,
                    onDeleteSheet = homeViewModel::deleteSheet
                )
            }
            composable(Destination.Progress.route) {
                ProgressScreen(
                    viewModel { ProgressViewModel(application.repository, application.pro.isPro) },
                    onOpenPro = { navController.navigate(Destination.Settings.route) { launchSingleTop = true } }
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(viewModel { SettingsViewModel(application.settings, application.backup) }, application.pro)
            }
            composable("sheet/{$SHEET_ID}", arguments = sheetArguments) { entry ->
                val sheetId = entry.arguments!!.getLong(SHEET_ID)
                val preferences by application.settings.preferences.collectAsStateWithLifecycle(AppPreferences())
                WorkoutScreen(
                    viewModel = viewModel { WorkoutViewModel(application.repository, sheetId) },
                    preferences = preferences,
                    onSearch = { navController.navigate("sheet/$sheetId/search") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("sheet/{$SHEET_ID}/search", arguments = sheetArguments) { entry ->
                val sheetId = entry.arguments!!.getLong(SHEET_ID)
                ExerciseSearchScreen(
                    viewModel = viewModel { ExerciseSearchViewModel(application.catalog, application.repository, sheetId) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
