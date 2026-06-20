package dev.bri.polarphases

import android.app.Application
import dev.bri.polarphases.data.db.AppDatabase
import dev.bri.polarphases.repository.ZoneRepository

class PolarPhasesApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val zoneRepository by lazy { ZoneRepository(database.hrZoneDao()) }
}
