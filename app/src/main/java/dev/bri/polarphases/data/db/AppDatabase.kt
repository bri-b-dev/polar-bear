package dev.bri.polarphases.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.bri.polarphases.data.model.BlockPhase
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.data.model.TemplateSequenceItem
import dev.bri.polarphases.data.model.WorkoutTemplate
import dev.bri.polarphases.util.defaultZones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HrZone::class,
        WorkoutTemplate::class,
        TemplateSequenceItem::class,
        BlockPhase::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hrZoneDao(): HrZoneDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS template_sequence_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        itemType TEXT NOT NULL,
                        phaseName TEXT,
                        durationSeconds INTEGER,
                        zoneId INTEGER,
                        repeatCount INTEGER,
                        FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_template_sequence_items_templateId ON template_sequence_items(templateId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS block_phases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sequenceItemId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        phaseName TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        zoneId INTEGER NOT NULL,
                        FOREIGN KEY(sequenceItemId) REFERENCES template_sequence_items(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_block_phases_sequenceItemId ON block_phases(sequenceItemId)"
                )
            }
        }

        // Rebuilds template_sequence_items and block_phases to replace zoneId (INTEGER) with
        // zoneIds (TEXT, comma-separated) so phases can reference multiple zones.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE template_sequence_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        itemType TEXT NOT NULL,
                        phaseName TEXT,
                        durationSeconds INTEGER,
                        zoneIds TEXT,
                        repeatCount INTEGER,
                        FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO template_sequence_items_new
                        (id, templateId, sortOrder, itemType, phaseName, durationSeconds, zoneIds, repeatCount)
                    SELECT id, templateId, sortOrder, itemType, phaseName, durationSeconds,
                           CASE WHEN zoneId IS NOT NULL THEN CAST(zoneId AS TEXT) ELSE NULL END,
                           repeatCount
                    FROM template_sequence_items
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE template_sequence_items")
                db.execSQL("ALTER TABLE template_sequence_items_new RENAME TO template_sequence_items")
                db.execSQL(
                    "CREATE INDEX index_template_sequence_items_templateId ON template_sequence_items(templateId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE block_phases_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sequenceItemId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        phaseName TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        zoneIds TEXT NOT NULL,
                        FOREIGN KEY(sequenceItemId) REFERENCES template_sequence_items(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO block_phases_new
                        (id, sequenceItemId, sortOrder, phaseName, durationSeconds, zoneIds)
                    SELECT id, sequenceItemId, sortOrder, phaseName, durationSeconds, CAST(zoneId AS TEXT)
                    FROM block_phases
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE block_phases")
                db.execSQL("ALTER TABLE block_phases_new RENAME TO block_phases")
                db.execSQL(
                    "CREATE INDEX index_block_phases_sequenceItemId ON block_phases(sequenceItemId)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polar_phases.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
