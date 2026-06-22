package dev.bri.polarphases.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSession,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val zoneSnapshots: List<ZoneSnapshot>,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val hrSamples: List<HrSample>,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val phaseRecords: List<SessionPhaseRecord>,
)
