package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_phases",
    foreignKeys = [
        ForeignKey(
            entity = TemplateSequenceItem::class,
            parentColumns = ["id"],
            childColumns = ["sequenceItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sequenceItemId")],
)
data class BlockPhase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceItemId: Long,
    val sortOrder: Int,
    val phaseName: String,
    val durationSeconds: Int,
    val zoneIds: String, // comma-separated zone IDs, never empty
)
