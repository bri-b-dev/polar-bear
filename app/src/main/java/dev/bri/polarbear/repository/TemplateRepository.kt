package dev.bri.polarbear.repository

import dev.bri.polarbear.data.db.WorkoutTemplateDao
import dev.bri.polarbear.data.model.BlockPhase
import dev.bri.polarbear.data.model.TemplateSequenceItem
import dev.bri.polarbear.data.model.WorkoutTemplate
import dev.bri.polarbear.data.model.WorkoutTemplateWithItems
import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val dao: WorkoutTemplateDao) {

    fun observeAll(): Flow<List<WorkoutTemplate>> = dao.observeAll()

    fun observeWithItems(id: Long): Flow<WorkoutTemplateWithItems?> = dao.observeWithItems(id)

    suspend fun createTemplate(name: String): Long =
        dao.insert(WorkoutTemplate(name = name))

    suspend fun renameTemplate(id: Long, name: String) =
        dao.update(WorkoutTemplate(id = id, name = name))

    suspend fun deleteTemplate(id: Long) = dao.deleteById(id)

    suspend fun deleteAllSequenceItemsForTemplate(templateId: Long) =
        dao.deleteAllSequenceItemsForTemplate(templateId)

    suspend fun addPhaseItem(
        templateId: Long,
        sortOrder: Int,
        name: String,
        durationSeconds: Int,
        zoneIds: List<Long>,
    ): Long = dao.insertSequenceItem(
        TemplateSequenceItem(
            templateId = templateId,
            sortOrder = sortOrder,
            itemType = ITEM_TYPE_PHASE,
            phaseName = name,
            durationSeconds = durationSeconds,
            zoneIds = zoneIds.joinToString(","),
        )
    )

    suspend fun addBlockItem(
        templateId: Long,
        sortOrder: Int,
        repeatCount: Int,
    ): Long = dao.insertSequenceItem(
        TemplateSequenceItem(
            templateId = templateId,
            sortOrder = sortOrder,
            itemType = ITEM_TYPE_BLOCK,
            repeatCount = repeatCount,
        )
    )

    suspend fun addBlockPhase(
        sequenceItemId: Long,
        sortOrder: Int,
        name: String,
        durationSeconds: Int,
        zoneIds: List<Long>,
    ) = dao.insertBlockPhase(
        BlockPhase(
            sequenceItemId = sequenceItemId,
            sortOrder = sortOrder,
            phaseName = name,
            durationSeconds = durationSeconds,
            zoneIds = zoneIds.joinToString(","),
        )
    )

    suspend fun duplicateTemplate(id: Long) {
        val source = dao.getWithItems(id) ?: return
        val newId = createTemplate("${source.template.name} (copy)")
        source.items.sortedBy { it.item.sortOrder }.forEachIndexed { idx, itemWithPhases ->
            when (itemWithPhases.item.itemType) {
                ITEM_TYPE_PHASE -> addPhaseItem(
                    templateId = newId,
                    sortOrder = idx,
                    name = itemWithPhases.item.phaseName ?: "",
                    durationSeconds = itemWithPhases.item.durationSeconds ?: 0,
                    zoneIds = itemWithPhases.item.zoneIds?.toZoneIdList() ?: emptyList(),
                )
                ITEM_TYPE_BLOCK -> {
                    val blockId = addBlockItem(
                        templateId = newId,
                        sortOrder = idx,
                        repeatCount = itemWithPhases.item.repeatCount ?: 1,
                    )
                    itemWithPhases.blockPhases.sortedBy { it.sortOrder }.forEachIndexed { pIdx, bp ->
                        addBlockPhase(
                            sequenceItemId = blockId,
                            sortOrder = pIdx,
                            name = bp.phaseName,
                            durationSeconds = bp.durationSeconds,
                            zoneIds = bp.zoneIds.toZoneIdList(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ITEM_TYPE_PHASE = "PHASE"
        const val ITEM_TYPE_BLOCK = "BLOCK"
    }
}

fun String.toZoneIdList(): List<Long> =
    if (isBlank()) emptyList()
    else split(",").mapNotNull { it.trim().toLongOrNull() }
