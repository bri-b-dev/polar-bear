package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_sequence_items",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("templateId")],
)
data class TemplateSequenceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val sortOrder: Int,
    val itemType: String, // "PHASE" or "BLOCK"
    val phaseName: String? = null,
    val durationSeconds: Int? = null,
    val zoneIds: String? = null, // comma-separated zone IDs, null for BLOCK items
    val repeatCount: Int? = null,
)
