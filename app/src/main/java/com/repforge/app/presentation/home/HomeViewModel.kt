package com.repforge.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.domain.templates.WorkoutTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val sheets: List<TrainingSheet> = emptyList(),
    val summary: DashboardSummary = DashboardSummary()
)

class HomeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        repository.observeSheets(), repository.observeSummary()
    ) { sheets, summary -> HomeUiState(sheets = sheets, summary = summary) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createSheet(name: String) {
        // The dialog only enables confirmation for a non-blank name, the single rule the repository enforces.
        viewModelScope.launch { runCatching { repository.createSheet(name) } }
    }

    fun createSheetFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { runCatching { repository.createSheetFromTemplate(template) } }
    }

    fun duplicateSheet(sheetId: Long) {
        viewModelScope.launch { repository.duplicateSheet(sheetId) }
    }

    fun deleteSheet(sheetId: Long) {
        viewModelScope.launch { repository.deleteSheet(sheetId) }
    }
}
