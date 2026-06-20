package dev.bri.polarphases.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.util.defaultZones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [HrZone::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hrZoneDao(): HrZoneDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polar_phases.db",
                )
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.hrZoneDao()
                    if (dao.count() == 0) {
                        defaultZones().forEachIndexed { index, zone ->
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
            }
        }
    }
}
