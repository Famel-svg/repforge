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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

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
