package com.repforge.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.metrics.Consistency
import com.repforge.app.domain.metrics.DayVolume
import com.repforge.app.domain.metrics.ExerciseHistory
import com.repforge.app.domain.metrics.ExerciseStats
import com.repforge.app.domain.metrics.OneRepMaxPoint
import com.repforge.app.domain.metrics.ProgressRange
import com.repforge.app.domain.metrics.MuscleInfo
import com.repforge.app.domain.metrics.MuscleSets
import com.repforge.app.domain.metrics.WorkoutDay
import com.repforge.app.domain.metrics.muscleLookup
import com.repforge.app.domain.metrics.weeklyMuscleVolume
import com.repforge.app.data.ExerciseCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.repforge.app.domain.metrics.working
import com.repforge.app.domain.metrics.consistency
import com.repforge.app.domain.metrics.exerciseStats
import com.repforge.app.domain.metrics.exercisesWithHistory
import com.repforge.app.domain.metrics.filterByRange
import com.repforge.app.domain.metrics.last7DaysVolume
import com.repforge.app.domain.metrics.localDate
import com.repforge.app.domain.metrics.locked
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
    val stats: ExerciseStats,
    val locked: Boolean = false
)

data class ProgressUiState(
    val consistency: Consistency = Consistency(),
    val week: List<DayVolume> = emptyList(),
    val weekSets: Int = 0,
    val exercises: List<ExerciseHistory> = emptyList(),
    val detail: ExerciseDetail? = null,
    val recent: List<WorkoutDay> = emptyList(),
    /** Index 0 = last 7 days; Pro adds the 3 previous weeks. */
    val muscleWeeks: List<List<MuscleSets>> = listOf(emptyList()),
    val muscleLocked: Boolean = true
)

class ProgressViewModel(repository: WorkoutRepository, isPro: StateFlow<Boolean>, catalog: ExerciseCatalog) : ViewModel() {
    private val muscles = flow { emit(muscleLookup(catalog.all().map { it.name to MuscleInfo(it.muscle, it.secondary) })) }
    private val selectedId = MutableStateFlow<Long?>(null)
    private val range = MutableStateFlow(ProgressRange.Days30)

    val state: StateFlow<ProgressUiState> = combine(repository.observeHistory(), selectedId, range, isPro, muscles) { history, id, r, pro, lookup ->
        buildState(history, id, r, pro, lookup, System.currentTimeMillis(), ZoneId.systemDefault())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun toggleExercise(exerciseId: Long) = selectedId.update { if (it == exerciseId) null else exerciseId }

    fun selectRange(value: ProgressRange) {
        range.value = value
    }

    private fun buildState(history: List<HistoryEntry>, id: Long?, r: ProgressRange, pro: Boolean, lookup: Map<String, MuscleInfo>, now: Long, zone: ZoneId): ProgressUiState {
        val exercises = exercisesWithHistory(history)
        val week = last7DaysVolume(history, now, zone)
        val weekStart = week.first().date
        val selected = exercises.firstOrNull { it.exerciseId == id }
        val detail = selected?.let {
            val own = history.filter { e -> e.exerciseId == it.exerciseId }
            val locked = r.locked(pro)
            ExerciseDetail(it, r, if (locked) emptyList() else filterByRange(oneRepMaxSeries(own, zone), r, localDate(now, zone)), exerciseStats(own), locked)
        }
        return ProgressUiState(
            consistency = consistency(history, now, zone),
            week = week,
            weekSets = history.working().filter { !localDate(it.recordedAt, zone).isBefore(weekStart) }.sumOf { it.sets },
            exercises = exercises,
            detail = detail,
            recent = recentWorkouts(history, zone),
            muscleWeeks = weeklyMuscleVolume(history, lookup, now, zone, if (pro) 4 else 1),
            muscleLocked = !pro
        )
    }
}
