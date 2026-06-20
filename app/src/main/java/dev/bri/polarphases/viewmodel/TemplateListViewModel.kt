package dev.bri.polarphases.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.data.model.WorkoutTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as PolarPhasesApp).templateRepository

    val templates: StateFlow<List<WorkoutTemplate>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repo.deleteTemplate(id) }
    }
}
