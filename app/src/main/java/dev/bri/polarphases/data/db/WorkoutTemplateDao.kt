package dev.bri.polarphases.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.bri.polarphases.data.model.BlockPhase
import dev.bri.polarphases.data.model.TemplateSequenceItem
import dev.bri.polarphases.data.model.WorkoutTemplate
import dev.bri.polarphases.data.model.WorkoutTemplateWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkoutTemplate>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun observeWithItems(id: Long): Flow<WorkoutTemplateWithItems?>

    @Insert
    suspend fun insert(template: WorkoutTemplate): Long

    @Update
    suspend fun update(template: WorkoutTemplate)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertSequenceItem(item: TemplateSequenceItem): Long

    @Insert
    suspend fun insertBlockPhase(phase: BlockPhase): Long
}
