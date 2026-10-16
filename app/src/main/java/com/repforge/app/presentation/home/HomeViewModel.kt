package com.repforge.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.presentation.logExceptions
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.domain.templates.WorkoutTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [sheet] null = rest day; [configured] false = no weekday has a sheet yet; [moved] = today comes from a one-off override. */
data class TodayState(val sheet: TrainingSheet?, val moved: Boolean, val configured: Boolean)

data class HomeUiState(
    val sheets: List<TrainingSheet> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val today: TodayState? = null,
    /** Sheet of the workout in progress, if any (null also when that sheet was deleted). */
    val resume: TrainingSheet? = null
)

class HomeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    // The date is read per collection so a long-lived process does not keep yesterday's plan.
    private val todayPlan = flow { emitAll(repository.observePlan(LocalDate.now(), 1)) }

    val state: StateFlow<HomeUiState> = combine(
        repository.observeSheets(), repository.observeSummary(), todayPlan, repository.observeWeeklyPlan(), repository.observeActiveSession()
    ) { sheets, summary, plan, weekly, active ->
        val day = plan.single()
        HomeUiState(sheets, summary, TodayState(sheets.firstOrNull { it.id == day.sheetId }, day.moved, weekly.values.any { it != null }), sheets.firstOrNull { it.id == active?.sheetId })
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** [bodyWeightKg] comes from the optional start prompt. A failure leaves the workout screen's own Start button as the fallback. */
    fun startSession(sheetId: Long, bodyWeightKg: Double?) {
        viewModelScope.launch(logExceptions) { repository.startSession(sheetId, bodyWeightKg) }
    }

    fun createSheet(name: String) {
        // The dialog only enables confirmation for a non-blank name, the single rule the repository enforces.
        viewModelScope.launch(logExceptions) { runCatching { repository.createSheet(name) } }
    }

    fun createSheetFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch(logExceptions) { runCatching { repository.createSheetFromTemplate(template) } }
    }

    fun duplicateSheet(sheetId: Long, copyLabel: String) {
        viewModelScope.launch(logExceptions) { repository.duplicateSheet(sheetId, copyLabel) }
    }

    fun deleteSheet(sheetId: Long) {
        viewModelScope.launch(logExceptions) { repository.deleteSheet(sheetId) }
    }
}
