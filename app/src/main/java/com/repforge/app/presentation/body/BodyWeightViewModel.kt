package com.repforge.app.presentation.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.presentation.logExceptions
import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BodyWeightViewModel(private val repository: WorkoutRepository) : ViewModel() {
    /** Newest first. */
    val weights: StateFlow<List<BodyWeight>> = repository.observeBodyWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(weightKg: Double) = viewModelScope.launch(logExceptions) { repository.addBodyWeight(weightKg) }

    fun delete(id: Long) = viewModelScope.launch(logExceptions) { repository.deleteBodyWeight(id) }
}
