package dev.bri.polarbear.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarbear.PolarBearApp
import dev.bri.polarbear.data.model.WorkoutSession
import dev.bri.polarbear.data.model.WorkoutSessionWithDetails
import dev.bri.polarbear.util.TcxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WorkoutHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = (application as PolarBearApp).sessionRepository

    val sessions: StateFlow<List<WorkoutSession>> = sessionRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteSession(id: Long) {
        viewModelScope.launch { sessionRepo.delete(id) }
    }
}

class WorkoutSessionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionRepo = (application as PolarBearApp).sessionRepository

    private val _details = MutableStateFlow<WorkoutSessionWithDetails?>(null)
    val details: StateFlow<WorkoutSessionWithDetails?> = _details.asStateFlow()

    private val _exportFilePath = MutableStateFlow<String?>(null)
    val exportFilePath: StateFlow<String?> = _exportFilePath.asStateFlow()

    private var job: Job? = null

    fun load(sessionId: Long) {
        job?.cancel()
        job = viewModelScope.launch {
            sessionRepo.observeWithDetails(sessionId).collect { _details.value = it }
        }
    }

    fun exportTcx(context: Context) {
        val data = _details.value ?: return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val tcx = TcxExporter.generate(data)
                val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val file = File(dir, "workout_${data.session.id}.tcx")
                file.writeText(tcx)
                file.absolutePath
            }
            _exportFilePath.value = path
        }
    }

    fun clearExportRequest() {
        _exportFilePath.value = null
    }
}
