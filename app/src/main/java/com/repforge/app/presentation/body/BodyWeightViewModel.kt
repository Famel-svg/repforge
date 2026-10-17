package com.repforge.app.presentation.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.presentation.logExceptions
import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [weightGoalKg] is the SettingsStore goal (`preferences.map { it.weightGoalKg }`); without it no goal line is drawn. */
class BodyWeightViewModel(private val repository: WorkoutRepository, weightGoalKg: Flow<Double?> = flowOf(null)) : ViewModel() {
    val goal: StateFlow<Double?> = weightGoalKg.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Newest first. */
    val weights: StateFlow<List<BodyWeight>> = repository.observeBodyWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(weightKg: Double) = viewModelScope.launch(logExceptions) { repository.addBodyWeight(weightKg) }

    fun delete(id: Long) = viewModelScope.launch(logExceptions) { repository.deleteBodyWeight(id) }
}
