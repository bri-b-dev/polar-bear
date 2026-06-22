package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateName: String,
    val startedAt: Long,
    val endedAt: Long,
    val endReason: String, // "COMPLETED" | "EARLY_EXIT"
    val totalPhasesPlanned: Int,
    val totalPhasesCompleted: Int,
)
