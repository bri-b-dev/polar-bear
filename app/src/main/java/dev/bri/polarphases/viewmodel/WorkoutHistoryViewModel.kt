package dev.bri.polarphases.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.data.model.WorkoutSession
import dev.bri.polarphases.data.model.WorkoutSessionWithDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = (application as PolarPhasesApp).sessionRepository

    val sessions: StateFlow<List<WorkoutSession>> = sessionRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteSession(id: Long) {
        viewModelScope.launch { sessionRepo.delete(id) }
    }
}

class WorkoutSessionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = (application as PolarPhasesApp).sessionRepository

    private val _details = MutableStateFlow<WorkoutSessionWithDetails?>(null)
    val details: StateFlow<WorkoutSessionWithDetails?> = _details.asStateFlow()

    private var job: Job? = null

    fun load(sessionId: Long) {
        job?.cancel()
        job = viewModelScope.launch {
            sessionRepo.observeWithDetails(sessionId).collect { _details.value = it }
        }
    }
}
