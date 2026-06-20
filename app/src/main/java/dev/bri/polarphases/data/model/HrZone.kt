package dev.bri.polarphases.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hr_zones")
data class HrZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val bpmMin: Int,
    val bpmMax: Int,
    val sortOrder: Int,
)
