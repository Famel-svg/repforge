package com.repforge.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.presentation.logExceptions
import com.repforge.app.domain.model.BodyWeight
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [sheet] null = rest day; [configured] false = no weekday has a sheet yet; [moved] = today comes from a one-off override. */
data class TodayState(val sheet: TrainingSheet?, val moved: Boolean, val configured: Boolean)

data class HomeUiState(
    val sheets: List<TrainingSheet> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val today: TodayState? = null,
    /** Sheet of the workout in progress, if any (null also when that sheet was deleted). */
    val resume: TrainingSheet? = null,
    /** Monday-to-Sunday strip for the week shown ([weekOffset] weeks from the current one). */
    val week: List<WeekDay> = emptyList(),
    val weekOffset: Int = 0,
    val bodyWeights: List<BodyWeight> = emptyList(),
    /** Distinct trained days per ISO week, keyed by that week's Monday. */
    val weeklyCounts: Map<LocalDate, Int> = emptyMap()
)

data class WeekDay(val date: LocalDate, val planned: Boolean, val trained: Boolean)

fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Consecutive weeks meeting [target] trained days, ending at the current week (or the previous one while the current is still in progress). */
fun streakWeeks(counts: Map<LocalDate, Int>, target: Int, thisMonday: LocalDate): Int {
    var week = if ((counts[thisMonday] ?: 0) >= target) thisMonday else thisMonday.minusWeeks(1)
    var n = 0
    while ((counts[week] ?: 0) >= target) { n++; week = week.minusWeeks(1) }
    return n
}

class HomeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    // The date is read per collection so a long-lived process does not keep yesterday's plan.
    private val todayPlan = flow { emitAll(repository.observePlan(LocalDate.now(), 1)) }

    private val weekOffset = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val weekPlan = weekOffset.flatMapLatest { off -> flow { emitAll(repository.observePlan(mondayOf(LocalDate.now()).plusWeeks(off.toLong()), 7)) } }

    private val base = combine(
        repository.observeSheets(), repository.observeSummary(), todayPlan, repository.observeWeeklyPlan(), repository.observeActiveSession()
    ) { sheets, summary, plan, weekly, active ->
        val day = plan.single()
        HomeUiState(sheets, summary, TodayState(sheets.firstOrNull { it.id == day.sheetId }, day.moved, weekly.values.any { it != null }), sheets.firstOrNull { it.id == active?.sheetId })
    }

    val state: StateFlow<HomeUiState> = combine(base, repository.observeHistory(), repository.observeBodyWeights(), weekPlan, weekOffset) { b, history, weights, plan, off ->
        val zone = ZoneId.systemDefault()
        val trained = history.map { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }.toSet()
        b.copy(
            week = plan.map { WeekDay(it.date, it.sheetId != null, it.date in trained) },
            weekOffset = off,
            bodyWeights = weights.sortedBy { it.recordedAt },
            weeklyCounts = trained.groupingBy { mondayOf(it) }.eachCount()
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun shiftWeek(delta: Int) = weekOffset.update { it + delta }

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
