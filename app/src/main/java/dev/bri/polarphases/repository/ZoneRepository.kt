package dev.bri.polarphases.repository

import dev.bri.polarphases.data.db.HrZoneDao
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.util.KarvonenZone
import kotlinx.coroutines.flow.Flow

class ZoneRepository(private val dao: HrZoneDao) {

    fun observeZones(): Flow<List<HrZone>> = dao.observeAll()

    suspend fun addZone(name: String, colorArgb: Int, bpmMin: Int, bpmMax: Int) {
        val sortOrder = dao.maxSortOrder() + 1
        dao.insert(HrZone(name = name, colorArgb = colorArgb, bpmMin = bpmMin, bpmMax = bpmMax, sortOrder = sortOrder))
    }

    suspend fun updateZone(zone: HrZone) = dao.update(zone)

    suspend fun deleteZone(id: Long) = dao.deleteById(id)

    /** Replaces all zones with the given Karvonen-computed list. */
    suspend fun replaceWithKarvonenZones(zones: List<KarvonenZone>) {
        dao.deleteAll()
        zones.forEachIndexed { index, zone ->
            dao.insert(
                HrZone(
                    name = zone.name,
                    colorArgb = zone.colorArgb,
                    bpmMin = zone.bpmMin,
                    bpmMax = zone.bpmMax,
                    sortOrder = index,
                )
            )
        }
    }
}
