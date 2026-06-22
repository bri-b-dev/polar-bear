package dev.bri.polarphases.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.bri.polarphases.data.model.BlockPhase
import dev.bri.polarphases.data.model.HrSample
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.data.model.SessionPhaseRecord
import dev.bri.polarphases.data.model.TemplateSequenceItem
import dev.bri.polarphases.data.model.WorkoutSession
import dev.bri.polarphases.data.model.WorkoutTemplate
import dev.bri.polarphases.data.model.ZoneSnapshot
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
        WorkoutSession::class,
        ZoneSnapshot::class,
        HrSample::class,
        SessionPhaseRecord::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hrZoneDao(): HrZoneDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateName TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL,
                        endReason TEXT NOT NULL,
                        totalPhasesPlanned INTEGER NOT NULL,
                        totalPhasesCompleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS zone_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        colorArgb INTEGER NOT NULL,
                        bpmMin INTEGER NOT NULL,
                        bpmMax INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_zone_snapshots_sessionId ON zone_snapshots(sessionId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hr_samples (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        elapsedMs INTEGER NOT NULL,
                        bpm INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_hr_samples_sessionId ON hr_samples(sessionId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_phase_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        phaseName TEXT NOT NULL,
                        plannedDurationSeconds INTEGER NOT NULL,
                        actualDurationSeconds INTEGER NOT NULL,
                        completionStatus TEXT NOT NULL,
                        blockId INTEGER,
                        blockRepIndex INTEGER,
                        blockTotalReps INTEGER,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_session_phase_records_sessionId ON session_phase_records(sessionId)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
