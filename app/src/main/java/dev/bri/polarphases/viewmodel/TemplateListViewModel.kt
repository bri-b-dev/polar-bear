package dev.bri.polarphases.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.data.model.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RenameDialogState(
    val templateId: Long,
    val name: String,
)

class TemplateListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as PolarPhasesApp).templateRepository

    val templates: StateFlow<List<WorkoutTemplate>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val renameDialog = MutableStateFlow<RenameDialogState?>(null)

    fun openRenameDialog(template: WorkoutTemplate) {
        renameDialog.value = RenameDialogState(templateId = template.id, name = template.name)
    }

    fun updateRenameName(name: String) {
        renameDialog.value = renameDialog.value?.copy(name = name)
    }

    fun dismissRenameDialog() {
        renameDialog.value = null
    }

    fun confirmRename() {
        val state = renameDialog.value ?: return
        if (state.name.isBlank()) return
        viewModelScope.launch {
            repo.renameTemplate(state.templateId, state.name.trim())
            renameDialog.value = null
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repo.deleteTemplate(id) }
    }
}
