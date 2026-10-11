package com.repforge.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.model.DailyVolume
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(val summary: DashboardSummary = DashboardSummary(), val days: List<DailyVolume> = emptyList())

class ProgressViewModel(repository: WorkoutRepository) : ViewModel() {
    val state: StateFlow<ProgressUiState> = combine(
        repository.observeSummary(), repository.observeDailyVolume()
    ) { summary, days -> ProgressUiState(summary, days) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}
