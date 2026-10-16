package com.repforge.app.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.domain.metrics.MuscleInfo
import com.repforge.app.domain.metrics.SessionSummary
import com.repforge.app.domain.metrics.muscleLookup
import com.repforge.app.domain.metrics.sessionSummary
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

/** Null until the session has ended and the history has loaded. */
class SessionSummaryViewModel(repository: WorkoutRepository, catalog: ExerciseCatalog, sessionId: Long) : ViewModel() {
    private val muscles = flow { emit(muscleLookup(catalog.all().map { it.name to MuscleInfo(it.muscle, it.secondary) })) }

    val state: StateFlow<SessionSummary?> = combine(repository.observeSession(sessionId), repository.observeHistory(), muscles) { session, history, lookup ->
        session?.endedAt?.let { sessionSummary(history, session.startedAt, it, lookup, ZoneId.systemDefault()) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
