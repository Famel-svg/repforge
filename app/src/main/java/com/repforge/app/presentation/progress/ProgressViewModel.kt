package com.repforge.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.metrics.Consistency
import com.repforge.app.domain.metrics.DayVolume
import com.repforge.app.domain.metrics.ExerciseHistory
import com.repforge.app.domain.metrics.ExerciseStats
import com.repforge.app.domain.metrics.OneRepMaxPoint
import com.repforge.app.domain.metrics.ProgressRange
import com.repforge.app.domain.metrics.WorkoutDay
import com.repforge.app.domain.metrics.consistency
import com.repforge.app.domain.metrics.exerciseStats
import com.repforge.app.domain.metrics.exercisesWithHistory
import com.repforge.app.domain.metrics.filterByRange
import com.repforge.app.domain.metrics.last7DaysVolume
import com.repforge.app.domain.metrics.localDate
import com.repforge.app.domain.metrics.oneRepMaxSeries
import com.repforge.app.domain.metrics.recentWorkouts
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZoneId

data class ExerciseDetail(
    val exercise: ExerciseHistory,
    val range: ProgressRange,
    val series: List<OneRepMaxPoint>,
    val stats: ExerciseStats
)

data class ProgressUiState(
    val consistency: Consistency = Consistency(),
    val week: List<DayVolume> = emptyList(),
    val weekSets: Int = 0,
    val exercises: List<ExerciseHistory> = emptyList(),
    val detail: ExerciseDetail? = null,
    val recent: List<WorkoutDay> = emptyList()
)

class ProgressViewModel(repository: WorkoutRepository) : ViewModel() {
    private val selectedId = MutableStateFlow<Long?>(null)
    private val range = MutableStateFlow(ProgressRange.Days30)

    val state: StateFlow<ProgressUiState> = combine(repository.observeHistory(), selectedId, range) { history, id, r ->
        buildState(history, id, r, System.currentTimeMillis(), ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun toggleExercise(exerciseId: Long) = selectedId.update { if (it == exerciseId) null else exerciseId }

    fun selectRange(value: ProgressRange) {
        range.value = value
    }

    private fun buildState(history: List<HistoryEntry>, id: Long?, r: ProgressRange, now: Long, zone: ZoneId): ProgressUiState {
        val exercises = exercisesWithHistory(history)
        val week = last7DaysVolume(history, now, zone)
        val weekStart = week.first().date
        val selected = exercises.firstOrNull { it.exerciseId == id }
        val detail = selected?.let {
            val own = history.filter { e -> e.exerciseId == it.exerciseId }
            ExerciseDetail(it, r, filterByRange(oneRepMaxSeries(own, zone), r, localDate(now, zone)), exerciseStats(own))
        }
        return ProgressUiState(
            consistency = consistency(history, now, zone),
            week = week,
            weekSets = history.filter { !localDate(it.recordedAt, zone).isBefore(weekStart) }.sumOf { it.sets },
            exercises = exercises,
            detail = detail,
            recent = recentWorkouts(history, zone)
        )
    }
}
