package dev.bri.polarphases.repository

import dev.bri.polarphases.data.db.WorkoutTemplateDao
import dev.bri.polarphases.data.model.BlockPhase
import dev.bri.polarphases.data.model.TemplateSequenceItem
import dev.bri.polarphases.data.model.WorkoutTemplate
import dev.bri.polarphases.data.model.WorkoutTemplateWithItems
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

    companion object {
        const val ITEM_TYPE_PHASE = "PHASE"
        const val ITEM_TYPE_BLOCK = "BLOCK"
    }
}

fun String.toZoneIdList(): List<Long> =
    if (isBlank()) emptyList()
    else split(",").mapNotNull { it.trim().toLongOrNull() }
