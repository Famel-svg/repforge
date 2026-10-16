package com.repforge.app.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.domain.schedule.DayPlan
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.presentation.logExceptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanUiState(
    val sheets: List<TrainingSheet> = emptyList(),
    val weekly: Map<Int, Long?> = emptyMap(),
    val upcoming: List<DayPlan> = emptyList(),
    val today: LocalDate = LocalDate.now()
)

class PlanViewModel(private val repository: WorkoutRepository) : ViewModel() {
    // "Today" is resolved per collection so a process kept alive overnight does not show yesterday's week.
    val state: StateFlow<PlanUiState> = flow {
        val today = LocalDate.now()
        emitAll(combine(repository.observeSheets(), repository.observeWeeklyPlan(), repository.observePlan(today, 7)) { sheets, weekly, upcoming ->
            PlanUiState(sheets, weekly, upcoming, today)
        })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setWeekday(dayOfWeek: Int, sheetId: Long?) {
        viewModelScope.launch(logExceptions) { repository.setWeekdaySheet(dayOfWeek, sheetId) }
    }

    fun reschedule(from: LocalDate, to: LocalDate) {
        viewModelScope.launch(logExceptions) { repository.reschedule(from, to) }
    }

    fun createSheet(name: String) {
        viewModelScope.launch(logExceptions) { runCatching { repository.createSheet(name) } }
    }

    fun createSheetFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch(logExceptions) { runCatching { repository.createSheetFromTemplate(template) } }
    }
}
