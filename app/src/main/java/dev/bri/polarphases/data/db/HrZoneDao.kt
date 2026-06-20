package dev.bri.polarphases.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.bri.polarphases.data.model.HrZone
import kotlinx.coroutines.flow.Flow

@Dao
interface HrZoneDao {
    @Query("SELECT * FROM hr_zones ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<HrZone>>

    @Insert
    suspend fun insert(zone: HrZone): Long

    @Update
    suspend fun update(zone: HrZone)

    @Query("DELETE FROM hr_zones WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM hr_zones")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM hr_zones")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM hr_zones")
    suspend fun deleteAll()
}
