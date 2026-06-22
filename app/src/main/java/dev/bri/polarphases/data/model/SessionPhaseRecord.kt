package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_phase_records",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class SessionPhaseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sortOrder: Int,
    val phaseName: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val completionStatus: String, // "COMPLETED" | "SKIPPED"
    val blockId: Long? = null,
    val blockRepIndex: Int? = null,
    val blockTotalReps: Int? = null,
)
