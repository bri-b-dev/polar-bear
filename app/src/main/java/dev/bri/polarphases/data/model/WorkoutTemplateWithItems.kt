package dev.bri.polarphases.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class TemplateSequenceItemWithPhases(
    @Embedded val item: TemplateSequenceItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "sequenceItemId",
    )
    val blockPhases: List<BlockPhase>,
)

data class WorkoutTemplateWithItems(
    @Embedded val template: WorkoutTemplate,
    @Relation(
        entity = TemplateSequenceItem::class,
        parentColumn = "id",
        entityColumn = "templateId",
    )
    val items: List<TemplateSequenceItemWithPhases>,
)
