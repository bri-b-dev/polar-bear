package dev.bri.polarphases.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import dev.bri.polarphases.data.model.HrSample
import dev.bri.polarphases.data.model.SessionPhaseRecord
import dev.bri.polarphases.data.model.WorkoutSession
import dev.bri.polarphases.data.model.WorkoutSessionWithDetails
import dev.bri.polarphases.data.model.ZoneSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeWithDetails(id: Long): Flow<List<WorkoutSessionWithDetails>>

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert
    suspend fun insertZoneSnapshots(snapshots: List<ZoneSnapshot>)

    @Insert
    suspend fun insertHrSamples(samples: List<HrSample>)

    @Insert
    suspend fun insertPhaseRecords(records: List<SessionPhaseRecord>)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
