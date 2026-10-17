package com.repforge.app.presentation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import com.repforge.app.R
import com.repforge.app.data.ExerciseCatalog
import androidx.compose.foundation.layout.padding
import com.repforge.app.presentation.library.LibraryScreen
import com.repforge.app.presentation.plan.PlanScreen
import com.repforge.app.presentation.plan.PlanViewModel
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.repforge.app.presentation.onboarding.OnboardingScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.repforge.app.RepForgeApplication
import com.repforge.app.data.AppPreferences
import com.repforge.app.presentation.body.BodyWeightScreen
import com.repforge.app.presentation.body.BodyWeightViewModel
import com.repforge.app.presentation.history.HistoryScreen
import com.repforge.app.presentation.history.HistoryViewModel
import com.repforge.app.presentation.home.HomeScreen
import com.repforge.app.presentation.start.BodyWeightPrompt
import com.repforge.app.presentation.start.StartChooserSheet
import com.repforge.app.domain.templates.StarterTemplates
import com.repforge.app.presentation.home.HomeViewModel
import com.repforge.app.presentation.progress.ProgressScreen
import com.repforge.app.presentation.progress.ProgressViewModel
import com.repforge.app.presentation.search.ExerciseSearchScreen
import com.repforge.app.presentation.search.ExerciseSearchViewModel
import com.repforge.app.presentation.settings.SettingsScreen
import com.repforge.app.presentation.summary.SessionSummaryScreen
import com.repforge.app.presentation.summary.SessionSummaryViewModel
import com.repforge.app.presentation.settings.SettingsViewModel
import com.repforge.app.presentation.workout.WorkoutScreen
import com.repforge.app.presentation.workout.WorkoutViewModel

private const val SETTINGS_ROUTE = "settings" // opened from the Home gear; not a tab

private const val SESSION_ID = "sessionId"
private const val SHEET_ID = "sheetId"
private val sheetArguments = listOf(navArgument(SHEET_ID) { type = NavType.LongType })

@Composable
fun RepForgeApp(initialSheetId: Long? = null) {
    val application = LocalContext.current.applicationContext as RepForgeApplication
    val navController = rememberNavController()
    // null until DataStore emits, so the start destination is never wrong for a frame.
    val onboardingDone by application.settings.onboardingDone.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    // A purchase can change outside the app (refund, Play Store redeem), so re-check every time the app comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { application.pro.restore() }
    val isPro by application.pro.isPro.collectAsStateWithLifecycle()
    // One instance for the whole app: the bottom bar's Start button and Home share it.
    val homeViewModel = viewModel { HomeViewModel(application.repository) }
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val preferences by application.settings.preferences.collectAsStateWithLifecycle(AppPreferences())
    var pendingStart by remember { mutableStateOf<Long?>(null) }
    var showChooser by remember { mutableStateOf(false) }
    // Saveable state must be declared before the early return below, with an explicit key: otherwise on
    // restore its positional key collides with rememberNavController's and the app crashes (ClassCastException).
    var widgetHandled by rememberSaveable(key = "widgetHandled") { mutableStateOf(false) }
    val catalogNames by produceState(emptyMap<String, String>()) {
        value = application.catalog.all().filter { it.nameEn.isNotBlank() }.associate { ExerciseCatalog.normalize(it.name) to it.nameEn }
    }
    if (onboardingDone == null) return
    val startRoute = remember { if (onboardingDone == true) Destination.Home.route else "onboarding" } // fixed at first value
    // Widget launch: once Home is up, push the sheet on top (only for a cold start, not after rotation).
    LaunchedEffect(initialSheetId, onboardingDone) {
        if (initialSheetId != null && !widgetHandled && onboardingDone == true) {
            widgetHandled = true
            navController.navigate("sheet/$initialSheetId")
        }
    }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    fun openSheet(id: Long) = navController.navigate("sheet/$id")
    fun startSheet(id: Long, kg: Double?) { homeViewModel.startSession(id, kg); openSheet(id) }
    // Resuming the same sheet never asks for body weight again.
    fun requestStart(id: Long) = when {
        homeState.resume?.id == id -> openSheet(id)
        preferences.askBodyWeight -> pendingStart = id
        else -> startSheet(id, null)
    }
    fun onStartTap() {
        val planned = homeState.today?.sheet
        when {
            homeState.resume != null -> openSheet(homeState.resume!!.id)
            planned != null -> requestStart(planned.id)
            else -> showChooser = true
        }
    }
    CompositionLocalProvider(LocalCatalogNames provides catalogNames) {
    Scaffold(
        bottomBar = {
            // Sheet and search are full-screen flows; the tab bar only belongs to top-level destinations (and Settings, which has no back button).
            if (currentRoute == SETTINGS_ROUTE || Destination.entries.any { it.route == currentRoute }) RepForgeBottomBar(
                currentRoute, resuming = homeState.resume != null, onStart = ::onStartTap,
                onTab = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(Destination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(navController, startDestination = startRoute, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("onboarding") {
                fun finish(withTemplate: Boolean) {
                    // Sheet creation lives in Plan now, so the starter template is created directly.
                    if (withTemplate) homeViewModel.createSheetFromTemplate(StarterTemplates.first())
                    scope.launch { application.settings.setOnboardingDone() }
                    navController.navigate(Destination.Home.route) { popUpTo("onboarding") { inclusive = true } }
                }
                OnboardingScreen(onFinish = { finish(false) }, onFinishWithTemplate = { finish(true) })
            }
            composable(Destination.Home.route) {
                HomeScreen(
                    state = homeState,
                    weightGoalKg = preferences.weightGoalKg,
                    weeklyTarget = preferences.weeklyTarget,
                    onStartSheet = { requestStart(it.id) },
                    onResumeSheet = { openSheet(it.id) },
                    onShiftWeek = homeViewModel::shiftWeek,
                    onOpenPlan = dropUnlessResumed { navController.navigate(Destination.Plan.route) { launchSingleTop = true } },
                    onOpenSettings = dropUnlessResumed { navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true } },
                    onOpenBody = dropUnlessResumed { navController.navigate("body") },
                    onOpenHistory = dropUnlessResumed { navController.navigate("history") }
                )
            }
            composable(Destination.Plan.route) {
                val planViewModel = viewModel { PlanViewModel(application.repository) }
                val state by planViewModel.state.collectAsStateWithLifecycle()
                PlanScreen(
                    state, planViewModel::setWeekday, planViewModel::reschedule, planViewModel::createSheet, planViewModel::createSheetFromTemplate,
                    onStartSheet = ::requestStart, onOpenSheet = ::openSheet
                )
            }
            composable("history") {
                HistoryScreen(
                    viewModel { HistoryViewModel(application.repository) },
                    onOpenSession = { navController.navigate("summary/$it") },
                    onBack = dropUnlessResumed { navController.popBackStack() }
                )
            }
            composable(Destination.Progress.route) {
                ProgressScreen(
                    viewModel { ProgressViewModel(application.repository, application.pro.isPro, application.catalog, application.settings.preferences.map { it.weightGoalKg }) },
                    onOpenPro = dropUnlessResumed { navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true } },
                    onOpenHistory = dropUnlessResumed { navController.navigate("history") }
                )
            }
            composable(Destination.Library.route) { LibraryScreen(application.catalog) }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(viewModel { SettingsViewModel(application.settings, application.backup) }, application.pro, onOpenBodyWeight = dropUnlessResumed { navController.navigate("body") })
            }
            composable("body") {
                BodyWeightScreen(viewModel { BodyWeightViewModel(application.repository, application.settings.preferences.map { it.weightGoalKg }) }, onBack = dropUnlessResumed { navController.popBackStack() })
            }
            composable("sheet/{$SHEET_ID}", arguments = sheetArguments) { entry ->
                val sheetId = entry.arguments!!.getLong(SHEET_ID)
                WorkoutScreen(
                    viewModel = viewModel { WorkoutViewModel(application.repository, sheetId) },
                    preferences = preferences,
                    onSearch = dropUnlessResumed { navController.navigate("sheet/$sheetId/search") },
                    isPro = isPro,
                    onOpenPro = dropUnlessResumed { navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true } },
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    // Fires after a database write, so a double tap must not push the summary twice.
                    onFinished = { id ->
                        if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) navController.navigate("summary/$id") { popUpTo("sheet/{$SHEET_ID}") { inclusive = true } }
                    }
                )
            }
            composable("summary/{$SESSION_ID}", arguments = listOf(navArgument(SESSION_ID) { type = NavType.LongType })) { entry ->
                val sessionId = entry.arguments!!.getLong(SESSION_ID)
                val done = dropUnlessResumed { navController.popBackStack() }
                SessionSummaryScreen(viewModel { SessionSummaryViewModel(application.repository, application.catalog, sessionId) }, onDone = done)
            }
            composable("sheet/{$SHEET_ID}/search", arguments = sheetArguments) { entry ->
                val sheetId = entry.arguments!!.getLong(SHEET_ID)
                ExerciseSearchScreen(
                    viewModel = viewModel { ExerciseSearchViewModel(application.catalog, application.repository, sheetId) },
                    onBack = dropUnlessResumed { navController.popBackStack() }
                )
            }
        }
    }
    pendingStart?.let { id ->
        BodyWeightPrompt(
            onSave = { kg -> pendingStart = null; startSheet(id, kg) },
            onSkip = { pendingStart = null; startSheet(id, null) },
            onDismiss = { pendingStart = null }
        )
    }
    if (showChooser) StartChooserSheet(
        sheets = homeState.sheets,
        onPick = { showChooser = false; requestStart(it.id) },
        onOpenPlan = { showChooser = false; navController.navigate(Destination.Plan.route) { launchSingleTop = true } },
        onDismiss = { showChooser = false }
    )
    }
}
