package dev.bri.polarphases.repository

import dev.bri.polarphases.data.db.WorkoutSessionDao
import dev.bri.polarphases.data.model.HrSample
import dev.bri.polarphases.data.model.SessionPhaseRecord
import dev.bri.polarphases.data.model.WorkoutSession
import dev.bri.polarphases.data.model.WorkoutSessionWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.bri.polarphases.data.model.ZoneSnapshot

class SessionRepository(private val dao: WorkoutSessionDao) {
    fun observeAll(): Flow<List<WorkoutSession>> = dao.observeAll()

    fun observeWithDetails(id: Long): Flow<WorkoutSessionWithDetails?> =
        dao.observeWithDetails(id).map { it.firstOrNull() }

    suspend fun save(
        session: WorkoutSession,
        zoneSnapshots: List<ZoneSnapshot>,
        hrSamples: List<HrSample>,
        phaseRecords: List<SessionPhaseRecord>,
    ): Long {
        val sessionId = dao.insertSession(session)
        if (zoneSnapshots.isNotEmpty()) {
            dao.insertZoneSnapshots(zoneSnapshots.map { it.copy(sessionId = sessionId) })
        }
        if (hrSamples.isNotEmpty()) {
            dao.insertHrSamples(hrSamples.map { it.copy(sessionId = sessionId) })
        }
        if (phaseRecords.isNotEmpty()) {
            dao.insertPhaseRecords(phaseRecords.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
