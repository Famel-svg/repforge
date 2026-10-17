package com.repforge.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.metrics.HistoryDay
import com.repforge.app.domain.metrics.groupByMonth
import com.repforge.app.domain.metrics.historyDays
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.ZoneId

data class HistoryUiState(val months: List<Pair<YearMonth, List<HistoryDay>>> = emptyList(), val loaded: Boolean = false)

class HistoryViewModel(repository: WorkoutRepository) : ViewModel() {
    val state: StateFlow<HistoryUiState> = combine(repository.observeFinishedSessions(), repository.observeHistory()) { sessions, history ->
        HistoryUiState(groupByMonth(historyDays(sessions, history, ZoneId.systemDefault())), loaded = true)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
