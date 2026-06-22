package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "zone_snapshots",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class ZoneSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val colorArgb: Int,
    val bpmMin: Int,
    val bpmMax: Int,
    val sortOrder: Int,
)
