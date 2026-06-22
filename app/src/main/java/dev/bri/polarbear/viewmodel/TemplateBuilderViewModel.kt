package dev.bri.polarbear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarbear.PolarBearApp
import dev.bri.polarbear.data.model.HrZone
import dev.bri.polarbear.repository.TemplateRepository.Companion.ITEM_TYPE_BLOCK
import dev.bri.polarbear.repository.TemplateRepository.Companion.ITEM_TYPE_PHASE
import dev.bri.polarbear.repository.toZoneIdList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhaseFormState(
    val name: String = "",
    val minutes: String = "5",
    val seconds: String = "00",
    val zoneIds: List<Long> = emptyList(),
    val editingIndex: Int? = null,
)

data class BlockPhaseForm(
    val name: String = "",
    val minutes: String = "5",
    val seconds: String = "00",
    val zoneIds: List<Long> = emptyList(),
)

data class BlockFormState(
    val repeatCount: String = "6",
    val phases: List<BlockPhaseForm> = listOf(BlockPhaseForm(), BlockPhaseForm()),
    val editingIndex: Int? = null,
)

sealed class SequenceItemDraft {
    data class Phase(
        val name: String,
        val durationSeconds: Int,
        val zoneIds: List<Long>,
        val zoneColors: List<Int>,
        val zoneNames: List<String>,
    ) : SequenceItemDraft()

    data class Block(
        val repeatCount: Int,
        val phases: List<PhaseDraft>,
    ) : SequenceItemDraft()

    data class PhaseDraft(
        val name: String,
        val durationSeconds: Int,
        val zoneIds: List<Long>,
        val zoneColors: List<Int>,
        val zoneNames: List<String>,
    )
}

class TemplateBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PolarBearApp
    private val templateRepo = app.templateRepository

    val zones: StateFlow<List<HrZone>> = app.zoneRepository.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templateName = MutableStateFlow("")
    val sequenceItems = MutableStateFlow<List<SequenceItemDraft>>(emptyList())
    val phaseDialog = MutableStateFlow<PhaseFormState?>(null)
    val blockDialog = MutableStateFlow<BlockFormState?>(null)
    val isSaved = MutableStateFlow(false)
    val validationError = MutableStateFlow<String?>(null)
    val editingTemplateId = MutableStateFlow<Long?>(null)

    fun loadTemplate(id: Long) {
        editingTemplateId.value = id
        viewModelScope.launch {
            val templateWithItems = templateRepo.observeWithItems(id).first() ?: return@launch
            val zoneList = zones.value.ifEmpty { zones.first { it.isNotEmpty() } }
            templateName.value = templateWithItems.template.name
            sequenceItems.value = templateWithItems.items
                .sortedBy { it.item.sortOrder }
                .mapNotNull { itemWithPhases ->
                    when (itemWithPhases.item.itemType) {
                        ITEM_TYPE_PHASE -> {
                            val ids = itemWithPhases.item.zoneIds?.toZoneIdList() ?: emptyList()
                            val resolved = ids.mapNotNull { zid -> zoneList.find { it.id == zid } }
                            SequenceItemDraft.Phase(
                                name = itemWithPhases.item.phaseName ?: "",
                                durationSeconds = itemWithPhases.item.durationSeconds ?: 0,
                                zoneIds = ids,
                                zoneColors = resolved.map { it.colorArgb },
                                zoneNames = resolved.map { it.name },
                            )
                        }
                        ITEM_TYPE_BLOCK -> SequenceItemDraft.Block(
                            repeatCount = itemWithPhases.item.repeatCount ?: 1,
                            phases = itemWithPhases.blockPhases
                                .sortedBy { it.sortOrder }
                                .map { bp ->
                                    val ids = bp.zoneIds.toZoneIdList()
                                    val resolved = ids.mapNotNull { zid -> zoneList.find { it.id == zid } }
                                    SequenceItemDraft.PhaseDraft(
                                        name = bp.phaseName,
                                        durationSeconds = bp.durationSeconds,
                                        zoneIds = ids,
                                        zoneColors = resolved.map { it.colorArgb },
                                        zoneNames = resolved.map { it.name },
                                    )
                                },
                        )
                        else -> null
                    }
                }
        }
    }

    fun updateTemplateName(name: String) { templateName.value = name }

    // ── Phase dialog ──────────────────────────────────────────────────────────

    fun openAddPhaseDialog() { phaseDialog.value = PhaseFormState() }

    fun openEditPhaseDialog(index: Int) {
        val item = sequenceItems.value.getOrNull(index) as? SequenceItemDraft.Phase ?: return
        val min = item.durationSeconds / 60
        val sec = item.durationSeconds % 60
        phaseDialog.value = PhaseFormState(
            name = item.name,
            minutes = min.toString(),
            seconds = "%02d".format(sec),
            zoneIds = item.zoneIds,
            editingIndex = index,
        )
    }

    fun updatePhaseForm(update: PhaseFormState.() -> PhaseFormState) {
        phaseDialog.value = phaseDialog.value?.update()
    }

    fun togglePhaseZone(zoneId: Long) {
        updatePhaseForm {
            copy(zoneIds = if (zoneId in zoneIds) zoneIds - zoneId else zoneIds + zoneId)
        }
    }

    fun confirmAddPhase() {
        val form = phaseDialog.value ?: return
        if (form.zoneIds.isEmpty()) return
        val min = form.minutes.toIntOrNull() ?: return
        val sec = form.seconds.toIntOrNull() ?: return
        if (form.name.isBlank() || min < 0 || sec < 0 || sec > 59 || (min == 0 && sec == 0)) return
        val orderedZones = form.zoneIds.mapNotNull { id -> zones.value.find { it.id == id } }
        val draft = SequenceItemDraft.Phase(
            name = form.name.trim(),
            durationSeconds = min * 60 + sec,
            zoneIds = form.zoneIds,
            zoneColors = orderedZones.map { it.colorArgb },
            zoneNames = orderedZones.map { it.name },
        )
        sequenceItems.value = form.editingIndex?.let { idx ->
            sequenceItems.value.toMutableList().apply { set(idx, draft) }
        } ?: sequenceItems.value + draft
        phaseDialog.value = null
    }

    fun dismissPhaseDialog() { phaseDialog.value = null }

    // ── Block dialog ──────────────────────────────────────────────────────────

    fun openAddBlockDialog() { blockDialog.value = BlockFormState() }

    fun openEditBlockDialog(index: Int) {
        val item = sequenceItems.value.getOrNull(index) as? SequenceItemDraft.Block ?: return
        blockDialog.value = BlockFormState(
            repeatCount = item.repeatCount.toString(),
            phases = item.phases.map { phase ->
                val min = phase.durationSeconds / 60
                val sec = phase.durationSeconds % 60
                BlockPhaseForm(
                    name = phase.name,
                    minutes = min.toString(),
                    seconds = "%02d".format(sec),
                    zoneIds = phase.zoneIds,
                )
            },
            editingIndex = index,
        )
    }

    fun updateBlockRepeatCount(count: String) {
        blockDialog.value = blockDialog.value?.copy(repeatCount = count.filter { it.isDigit() })
    }

    fun updateBlockPhase(index: Int, update: BlockPhaseForm.() -> BlockPhaseForm) {
        val current = blockDialog.value ?: return
        val phases = current.phases.toMutableList()
        if (index in phases.indices) phases[index] = phases[index].update()
        blockDialog.value = current.copy(phases = phases)
    }

    fun toggleBlockPhaseZone(phaseIndex: Int, zoneId: Long) {
        updateBlockPhase(phaseIndex) {
            copy(zoneIds = if (zoneId in zoneIds) zoneIds - zoneId else zoneIds + zoneId)
        }
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
            if (phaseForm.zoneIds.isEmpty()) return@mapNotNull null
            val min = phaseForm.minutes.toIntOrNull() ?: return@mapNotNull null
            val sec = phaseForm.seconds.toIntOrNull() ?: return@mapNotNull null
            if (phaseForm.name.isBlank() || min < 0 || sec < 0 || sec > 59 || (min == 0 && sec == 0)) return@mapNotNull null
            val orderedZones = phaseForm.zoneIds.mapNotNull { id -> zones.value.find { it.id == id } }
            SequenceItemDraft.PhaseDraft(
                name = phaseForm.name.trim(),
                durationSeconds = min * 60 + sec,
                zoneIds = phaseForm.zoneIds,
                zoneColors = orderedZones.map { it.colorArgb },
                zoneNames = orderedZones.map { it.name },
            )
        }
        if (resolvedPhases.size < 2) return
        val draft = SequenceItemDraft.Block(repeatCount = count, phases = resolvedPhases)
        sequenceItems.value = form.editingIndex?.let { idx ->
            sequenceItems.value.toMutableList().apply { set(idx, draft) }
        } ?: sequenceItems.value + draft
        blockDialog.value = null
    }

    fun dismissBlockDialog() { blockDialog.value = null }

    fun removeSequenceItem(index: Int) {
        sequenceItems.value = sequenceItems.value.filterIndexed { i, _ -> i != index }
    }

    fun moveItemUp(index: Int) {
        if (index <= 0) return
        val list = sequenceItems.value.toMutableList()
        val tmp = list[index - 1]; list[index - 1] = list[index]; list[index] = tmp
        sequenceItems.value = list
    }

    fun moveItemDown(index: Int) {
        val list = sequenceItems.value
        if (index >= list.size - 1) return
        val mutable = list.toMutableList()
        val tmp = mutable[index + 1]; mutable[index + 1] = mutable[index]; mutable[index] = tmp
        sequenceItems.value = mutable
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveTemplate() {
        val name = templateName.value.trim()
        if (name.isBlank()) { validationError.value = "Please enter a template name."; return }
        val items = sequenceItems.value
        if (items.isEmpty()) { validationError.value = "Add at least one phase or block."; return }
        validationError.value = null
        viewModelScope.launch {
            val existingId = editingTemplateId.value
            if (existingId != null) {
                templateRepo.renameTemplate(existingId, name)
                templateRepo.deleteAllSequenceItemsForTemplate(existingId)
                insertItems(existingId, items)
            } else {
                val templateId = templateRepo.createTemplate(name)
                insertItems(templateId, items)
            }
            isSaved.value = true
        }
    }

    private suspend fun insertItems(templateId: Long, items: List<SequenceItemDraft>) {
        items.forEachIndexed { index, item ->
            when (item) {
                is SequenceItemDraft.Phase -> templateRepo.addPhaseItem(
                    templateId = templateId,
                    sortOrder = index,
                    name = item.name,
                    durationSeconds = item.durationSeconds,
                    zoneIds = item.zoneIds,
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
                            zoneIds = phase.zoneIds,
                        )
                    }
                }
            }
        }
    }
}
