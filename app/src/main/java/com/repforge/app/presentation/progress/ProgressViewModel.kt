package com.repforge.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.metrics.Consistency
import com.repforge.app.domain.metrics.DayVolume
import com.repforge.app.domain.metrics.ExerciseHistory
import com.repforge.app.domain.metrics.ExerciseStats
import com.repforge.app.domain.metrics.ProgressPoint
import com.repforge.app.domain.metrics.ProgressRange
import com.repforge.app.domain.metrics.MuscleInfo
import com.repforge.app.domain.metrics.MuscleSets
import com.repforge.app.domain.metrics.BodyRange
import com.repforge.app.domain.metrics.Heatmap
import com.repforge.app.domain.metrics.activityHeatmap
import com.repforge.app.domain.metrics.bodyMuscleTotals
import com.repforge.app.domain.metrics.weeksSpanned
import com.repforge.app.domain.metrics.WorkoutDay
import com.repforge.app.domain.metrics.WeightRange
import com.repforge.app.domain.metrics.filterWeights
import com.repforge.app.domain.metrics.weightDelta
import com.repforge.app.domain.model.BodyWeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
import com.repforge.app.domain.metrics.progressSeries
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
    val series: List<ProgressPoint>,
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
    val muscleLocked: Boolean = true,
    val heatmap: Heatmap = Heatmap(emptyList(), 0),
    /** Sets per body muscle by range; free users only get [BodyRange.Week]. */
    val bodyMap: Map<BodyRange, Map<String, Double>> = emptyMap(),
    /** Body weights inside [weightRange], oldest first. */
    val weights: List<BodyWeight> = emptyList(),
    val weightRange: WeightRange = WeightRange.Days90,
    val weightGoalKg: Double? = null,
    val latestWeightKg: Double? = null,
    /** Change across [weights]; null with fewer than two records. */
    val weightDeltaKg: Double? = null
)

private class WeightInfo(val shown: List<BodyWeight>, val range: WeightRange, val goal: Double?, val latest: Double?, val delta: Double?)

/** [weightGoalKg] is the SettingsStore goal (`preferences.map { it.weightGoalKg }`); without it no goal line is drawn. */
class ProgressViewModel(
    repository: WorkoutRepository, isPro: StateFlow<Boolean>, catalog: ExerciseCatalog,
    weightGoalKg: Flow<Double?> = flowOf(null)
) : ViewModel() {
    private val muscles = flow { emit(muscleLookup(catalog.all().map { it.name to MuscleInfo(it.muscle, it.secondary) })) }
    private val selectedId = MutableStateFlow<Long?>(null)
    private val range = MutableStateFlow(ProgressRange.Days30)
    private val weightRange = MutableStateFlow(WeightRange.Days90)

    private val base = combine(repository.observeHistory(), selectedId, range, isPro, muscles) { history, id, r, pro, lookup ->
        buildState(history, id, r, pro, lookup, System.currentTimeMillis(), ZoneId.systemDefault())
    }
    private val weightInfo = combine(repository.observeBodyWeights(), weightGoalKg, weightRange) { all, goal, r ->
        val chrono = all.asReversed()
        val shown = filterWeights(chrono, r, System.currentTimeMillis(), ZoneId.systemDefault())
        WeightInfo(shown, r, goal, chrono.lastOrNull()?.weightKg, weightDelta(shown))
    }

    val state: StateFlow<ProgressUiState> = combine(base, weightInfo) { s, w -> s.copy(weights = w.shown, weightRange = w.range, weightGoalKg = w.goal, latestWeightKg = w.latest, weightDeltaKg = w.delta) }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun toggleExercise(exerciseId: Long) = selectedId.update { if (it == exerciseId) null else exerciseId }

    fun selectWeightRange(value: WeightRange) {
        weightRange.value = value
    }

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
            ExerciseDetail(it, r, if (locked) emptyList() else filterByRange(progressSeries(own, zone), r, localDate(now, zone)), exerciseStats(own), locked)
        }
        return ProgressUiState(
            consistency = consistency(history, now, zone),
            week = week,
            weekSets = history.working().filter { !localDate(it.recordedAt, zone).isBefore(weekStart) }.sumOf { it.sets },
            exercises = exercises,
            detail = detail,
            recent = recentWorkouts(history, zone),
            muscleWeeks = weeklyMuscleVolume(history, lookup, now, zone, if (pro) 4 else 1),
            muscleLocked = !pro,
            heatmap = activityHeatmap(history, now, zone),
            bodyMap = BodyRange.entries.filter { pro || !it.pro }.associateWith { br ->
                val weeks = when (br) { BodyRange.Week -> 1; BodyRange.Month -> 4; BodyRange.All -> weeksSpanned(history, now, zone) }
                bodyMuscleTotals(weeklyMuscleVolume(history, lookup, now, zone, weeks))
            }
        )
    }
}
