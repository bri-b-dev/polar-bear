package dev.bri.polarphases.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.data.model.HrZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhaseFormState(
    val name: String = "",
    val minutes: String = "0",
    val seconds: String = "30",
    val zoneId: Long? = null,
)

data class BlockPhaseForm(
    val name: String = "",
    val minutes: String = "0",
    val seconds: String = "30",
    val zoneId: Long? = null,
)

data class BlockFormState(
    val repeatCount: String = "6",
    val phases: List<BlockPhaseForm> = listOf(BlockPhaseForm(), BlockPhaseForm()),
)

sealed class SequenceItemDraft {
    data class Phase(
        val name: String,
        val durationSeconds: Int,
        val zoneId: Long,
        val zoneName: String,
        val zoneColorArgb: Int,
    ) : SequenceItemDraft()

    data class Block(
        val repeatCount: Int,
        val phases: List<PhaseDraft>,
    ) : SequenceItemDraft()

    data class PhaseDraft(
        val name: String,
        val durationSeconds: Int,
        val zoneId: Long,
        val zoneName: String,
        val zoneColorArgb: Int,
    )
}

class TemplateBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PolarPhasesApp
    private val templateRepo = app.templateRepository

    val zones: StateFlow<List<HrZone>> = app.zoneRepository.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templateName = MutableStateFlow("")
    val sequenceItems = MutableStateFlow<List<SequenceItemDraft>>(emptyList())
    val phaseDialog = MutableStateFlow<PhaseFormState?>(null)
    val blockDialog = MutableStateFlow<BlockFormState?>(null)
    val isSaved = MutableStateFlow(false)
    val validationError = MutableStateFlow<String?>(null)

    fun updateTemplateName(name: String) { templateName.value = name }

    fun openAddPhaseDialog() { phaseDialog.value = PhaseFormState() }

    fun updatePhaseForm(update: PhaseFormState.() -> PhaseFormState) {
        phaseDialog.value = phaseDialog.value?.update()
    }

    fun confirmAddPhase() {
        val form = phaseDialog.value ?: return
        val zone = zones.value.find { it.id == form.zoneId } ?: return
        val min = form.minutes.toIntOrNull() ?: return
        val sec = form.seconds.toIntOrNull() ?: return
        if (form.name.isBlank() || min < 0 || sec < 0 || sec > 59 || (min == 0 && sec == 0)) return
        sequenceItems.value = sequenceItems.value + SequenceItemDraft.Phase(
            name = form.name.trim(),
            durationSeconds = min * 60 + sec,
            zoneId = zone.id,
            zoneName = zone.name,
            zoneColorArgb = zone.colorArgb,
        )
        phaseDialog.value = null
    }

    fun dismissPhaseDialog() { phaseDialog.value = null }

    fun openAddBlockDialog() { blockDialog.value = BlockFormState() }

    fun updateBlockRepeatCount(count: String) {
        blockDialog.value = blockDialog.value?.copy(repeatCount = count.filter { it.isDigit() })
    }

    fun updateBlockPhase(index: Int, update: BlockPhaseForm.() -> BlockPhaseForm) {
        val current = blockDialog.value ?: return
        val phases = current.phases.toMutableList()
        if (index in phases.indices) phases[index] = phases[index].update()
        blockDialog.value = current.copy(phases = phases)
    }

    fun addPhaseToBlock() {
        val current = blockDialog.value ?: return
        blockDialog.value = current.copy(phases = current.phases + BlockPhaseForm())
    }

    fun removePhaseFromBlock(index: Int) {
        val current = blockDialog.value ?: return
        if (current.phases.size <= 2) return
        blockDialog.value = current.copy(
            phases = current.phases.filterIndexed { i, _ -> i != index }
        )
    }

    fun confirmAddBlock() {
        val form = blockDialog.value ?: return
        val count = form.repeatCount.toIntOrNull()
        if (count == null || count < 1) return
        val resolvedPhases = form.phases.mapNotNull { phaseForm ->
            val zone = zones.value.find { it.id == phaseForm.zoneId } ?: return@mapNotNull null
            val min = phaseForm.minutes.toIntOrNull() ?: return@mapNotNull null
            val sec = phaseForm.seconds.toIntOrNull() ?: return@mapNotNull null
            if (phaseForm.name.isBlank() || min < 0 || sec < 0 || sec > 59 || (min == 0 && sec == 0)) return@mapNotNull null
            SequenceItemDraft.PhaseDraft(
                name = phaseForm.name.trim(),
                durationSeconds = min * 60 + sec,
                zoneId = zone.id,
                zoneName = zone.name,
                zoneColorArgb = zone.colorArgb,
            )
        }
        if (resolvedPhases.size < 2) return
        sequenceItems.value = sequenceItems.value + SequenceItemDraft.Block(
            repeatCount = count,
            phases = resolvedPhases,
        )
        blockDialog.value = null
    }

    fun dismissBlockDialog() { blockDialog.value = null }

    fun removeSequenceItem(index: Int) {
        sequenceItems.value = sequenceItems.value.filterIndexed { i, _ -> i != index }
    }

    fun saveTemplate() {
        val name = templateName.value.trim()
        if (name.isBlank()) {
            validationError.value = "Please enter a template name."
            return
        }
        val items = sequenceItems.value
        if (items.isEmpty()) {
            validationError.value = "Add at least one phase or block."
            return
        }
        validationError.value = null
        viewModelScope.launch {
            val templateId = templateRepo.createTemplate(name)
            items.forEachIndexed { index, item ->
                when (item) {
                    is SequenceItemDraft.Phase -> templateRepo.addPhaseItem(
                        templateId = templateId,
                        sortOrder = index,
                        name = item.name,
                        durationSeconds = item.durationSeconds,
                        zoneId = item.zoneId,
                    )
                    is SequenceItemDraft.Block -> {
                        val blockId = templateRepo.addBlockItem(
                            templateId = templateId,
                            sortOrder = index,
                            repeatCount = item.repeatCount,
                        )
                        item.phases.forEachIndexed { phaseIndex, phase ->
                            templateRepo.addBlockPhase(
                                sequenceItemId = blockId,
                                sortOrder = phaseIndex,
                                name = phase.name,
                                durationSeconds = phase.durationSeconds,
                                zoneId = phase.zoneId,
                            )
                        }
                    }
                }
            }
            isSaved.value = true
        }
    }
}
