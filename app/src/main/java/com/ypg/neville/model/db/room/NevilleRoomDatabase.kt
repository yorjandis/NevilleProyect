package com.ypg.neville.model.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ypg.neville.feature.calmspace.data.CalmPersonalPhraseDao
import com.ypg.neville.feature.calmspace.data.CalmPersonalPhraseEntity
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchorDao
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchorEntity
import com.ypg.neville.feature.morningdialog.data.MorningDialogDao
import com.ypg.neville.feature.morningdialog.data.MorningDialogSessionEntity
import com.ypg.neville.feature.morningdialog.data.RitualDiaryExportDao
import com.ypg.neville.feature.morningdialog.data.RitualDiaryExportEntity
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryDao
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEventEntity
import com.ypg.neville.feature.weeklysummary.data.WeeklySummarySectionOrderEntity
import com.ypg.neville.feature.voice.data.VoiceRecordingDao
import com.ypg.neville.feature.voice.data.VoiceRecordingEntity
import com.ypg.neville.model.reminders.ReminderDao
import com.ypg.neville.model.reminders.ReminderEntity

@Database(
    entities = [
        NotaEntity::class,
        ReflexionEntity::class,
        FraseEntity::class,
        ConfEntity::class,
        DiarioEntity::class,
        GoalEntity::class,
        GoalUnitEntity::class,
        ArchivedGoalEntity::class,
        ArchivedUnitEntity::class,
        ReminderEntity::class,
        VoiceRecordingEntity::class,
        EmotionalAnchorEntity::class,
        PreferenceEntity::class,
        MorningDialogSessionEntity::class,
        RitualDiaryExportEntity::class,
        WeeklySummaryEntity::class,
        WeeklySummaryEventEntity::class,
        WeeklySummarySectionOrderEntity::class,
        CalmPersonalPhraseEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class NevilleRoomDatabase : RoomDatabase() {

    abstract fun notaDao(): NotaDao
    abstract fun reflexionDao(): ReflexionDao
    abstract fun fraseDao(): FraseDao
    abstract fun confDao(): ConfDao
    abstract fun diarioDao(): DiarioDao
    abstract fun goalDao(): GoalDao
    abstract fun goalUnitDao(): GoalUnitDao
    abstract fun archivedGoalDao(): ArchivedGoalDao
    abstract fun archivedUnitDao(): ArchivedUnitDao
    abstract fun reminderDao(): ReminderDao
    abstract fun voiceRecordingDao(): VoiceRecordingDao
    abstract fun emotionalAnchorDao(): EmotionalAnchorDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun morningDialogDao(): MorningDialogDao
    abstract fun ritualDiaryExportDao(): RitualDiaryExportDao
    abstract fun weeklySummaryDao(): WeeklySummaryDao
    abstract fun calmPersonalPhraseDao(): CalmPersonalPhraseDao

    companion object {
        @Volatile
        private var INSTANCE: NevilleRoomDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `frases` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`frase` TEXT NOT NULL, " +
                        "`autor` TEXT NOT NULL, " +
                        "`fuente` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`inbuild` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_frases_frase` ON `frases` (`frase`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `repo` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repo_title` ON `repo` (`title`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repo_link` ON `repo` (`link`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `videos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_videos_title` ON `videos` (`title`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_videos_link` ON `videos` (`link`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `conf` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_conf_title` ON `conf` (`title`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_conf_link` ON `conf` (`link`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `videos`")
                db.execSQL("DROP TABLE IF EXISTS `repo`")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Diario` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`emocion` TEXT NOT NULL, " +
                        "`fecha` INTEGER NOT NULL, " +
                        "`fechaM` INTEGER NOT NULL, " +
                        "`isFav` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notas` ADD COLUMN `isFav` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `frases_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`frase` TEXT NOT NULL, " +
                        "`autor` TEXT NOT NULL, " +
                        "`fuente` TEXT NOT NULL, " +
                        "`isfav` TEXT NOT NULL, " +
                        "`personal` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`inbuild` TEXT NOT NULL, " +
                        "`categoria` TEXT NOT NULL, " +
                        "`asset_key` TEXT NOT NULL, " +
                        "`asset_hash` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )

                db.execSQL(
                    "INSERT INTO `frases_new` (`id`, `frase`, `autor`, `fuente`, `isfav`, `personal`, `fav`, `nota`, `inbuild`, `categoria`, `asset_key`, `asset_hash`, `shared`) " +
                        "SELECT `id`, `frase`, `autor`, `fuente`, `fav`, CASE WHEN `inbuild` = '0' THEN '1' ELSE '0' END, `fav`, `nota`, `inbuild`, " +
                        "CASE WHEN LOWER(`autor`) = 'salud' THEN 'SALUD' ELSE 'AUTOR' END, '', '', `shared` " +
                        "FROM `frases`"
                )

                db.execSQL("DROP TABLE `frases`")
                db.execSQL("ALTER TABLE `frases_new` RENAME TO `frases`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_frases_asset_key` ON `frases` (`asset_key`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_frases_autor` ON `frases` (`autor`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_frases_categoria` ON `frases` (`categoria`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`descriptionText` TEXT NOT NULL, " +
                        "`totalUnits` INTEGER NOT NULL, " +
                        "`unitType` TEXT NOT NULL, " +
                        "`frequency` INTEGER NOT NULL, " +
                        "`isStarted` INTEGER NOT NULL, " +
                        "`startDate` INTEGER, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_title` ON `goals` (`title`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goal_units` (" +
                        "`id` TEXT NOT NULL, " +
                        "`goalId` TEXT NOT NULL, " +
                        "`unitIndex` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`unitType` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`info` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`startDate` INTEGER, " +
                        "`endDate` INTEGER, " +
                        "`completedDate` INTEGER, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_units_goalId` ON `goal_units` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_units_status` ON `goal_units` (`status`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `archived_goals` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`descriptionText` TEXT NOT NULL, " +
                        "`totalUnits` INTEGER NOT NULL, " +
                        "`unitType` TEXT NOT NULL, " +
                        "`frequency` INTEGER NOT NULL, " +
                        "`completionDate` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_archived_goals_completionDate` ON `archived_goals` (`completionDate`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `archived_units` (" +
                        "`id` TEXT NOT NULL, " +
                        "`goalId` TEXT NOT NULL, " +
                        "`unitIndex` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`info` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`startDate` INTEGER, " +
                        "`endDate` INTEGER, " +
                        "`completedDate` INTEGER, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`goalId`) REFERENCES `archived_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_archived_units_goalId` ON `archived_units` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_archived_units_status` ON `archived_units` (`status`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`message` TEXT NOT NULL, " +
                        "`frequencyType` TEXT NOT NULL, " +
                        "`intervalHours` INTEGER, " +
                        "`intervalMinutes` INTEGER, " +
                        "`dailyHour` INTEGER, " +
                        "`dailyMinute` INTEGER, " +
                        "`dateTimeMillis` INTEGER, " +
                        "`monthlyDay` INTEGER, " +
                        "`monthlyHour` INTEGER, " +
                        "`monthlyMinute` INTEGER, " +
                        "`yearlyMonth` INTEGER, " +
                        "`yearlyDay` INTEGER, " +
                        "`yearlyHour` INTEGER, " +
                        "`yearlyMinute` INTEGER, " +
                        "`isStarted` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER, " +
                        "`isPinned` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_sortOrder` ON `reminders` (`sortOrder`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `voice_recordings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`filePath` TEXT NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_voice_recordings_createdAt` ON `voice_recordings` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_voice_recordings_updatedAt` ON `voice_recordings` (`updatedAt`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `goals` ADD COLUMN `notifyOnUnitAvailable` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `goals` ADD COLUMN `lastNotifiedUnitIndex` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `emotional_anchors` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`phrase` TEXT NOT NULL, " +
                        "`breathingTechniqueId` TEXT NOT NULL, " +
                        "`breathingTechniqueName` TEXT NOT NULL, " +
                        "`breathingTechniquePattern` TEXT NOT NULL, " +
                        "`breathingTechniqueGuide` TEXT NOT NULL, " +
                        "`imagePath` TEXT NOT NULL, " +
                        "`audioPath` TEXT NOT NULL, " +
                        "`audioDurationMs` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_emotional_anchors_createdAt` ON `emotional_anchors` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_emotional_anchors_updatedAt` ON `emotional_anchors` (`updatedAt`)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `preferences` (" +
                        "`prefKey` TEXT NOT NULL, " +
                        "`prefValue` TEXT NOT NULL, " +
                        "`valueType` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`prefKey`))"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reflexiones_personales` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`contenido` TEXT NOT NULL, " +
                        "`favorito` INTEGER NOT NULL DEFAULT 0, " +
                        "`nota` TEXT NOT NULL DEFAULT '', " +
                        "`fechaCreacion` INTEGER NOT NULL, " +
                        "`fechaModificacion` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `morning_dialog_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionDateEpochDay` INTEGER NOT NULL, " +
                        "`completedAtEpochMillis` INTEGER NOT NULL, " +
                        "`goalsJson` TEXT NOT NULL, " +
                        "`identity` TEXT NOT NULL, " +
                        "`emotionsJson` TEXT NOT NULL, " +
                        "`anticipatedSituationsJson` TEXT NOT NULL, " +
                        "`consciousResponsesJson` TEXT NOT NULL, " +
                        "`completed` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_morning_dialog_sessions_sessionDateEpochDay` " +
                        "ON `morning_dialog_sessions` (`sessionDateEpochDay`)"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `morning_dialog_sessions` " +
                        "ADD COLUMN `noteText` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ritual_diary_exports` (" +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`diarioId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sessionId`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ritual_diary_exports_diarioId` " +
                        "ON `ritual_diary_exports` (`diarioId`)"
                )
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_summaries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`weekStartMillis` INTEGER NOT NULL, " +
                        "`weekEndMillis` INTEGER NOT NULL, " +
                        "`generatedAtMillis` INTEGER NOT NULL, " +
                        "`notesCreated` INTEGER NOT NULL, " +
                        "`notesModified` INTEGER NOT NULL, " +
                        "`notesDeleted` INTEGER NOT NULL, " +
                        "`journalCreated` INTEGER NOT NULL, " +
                        "`journalModified` INTEGER NOT NULL, " +
                        "`journalDeleted` INTEGER NOT NULL, " +
                        "`conferencesRead` INTEGER NOT NULL, " +
                        "`goalsCreated` INTEGER NOT NULL, " +
                        "`goalsCompleted` INTEGER NOT NULL, " +
                        "`goalsInProgress` INTEGER NOT NULL, " +
                        "`remindersCreated` INTEGER NOT NULL, " +
                        "`remindersModified` INTEGER NOT NULL, " +
                        "`remindersDeleted` INTEGER NOT NULL, " +
                        "`voiceCreated` INTEGER NOT NULL, " +
                        "`voiceDeleted` INTEGER NOT NULL, " +
                        "`emotionalAnchorsCreated` INTEGER NOT NULL, " +
                        "`emotionalAnchorsUsed` INTEGER NOT NULL, " +
                        "`morningRitualsCompleted` INTEGER NOT NULL, " +
                        "`personalPhrasesCreated` INTEGER NOT NULL, " +
                        "`personalPhrasesModified` INTEGER NOT NULL, " +
                        "`personalPhrasesDeleted` INTEGER NOT NULL, " +
                        "`encyclopediaAccessed` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_weekly_summaries_weekStartMillis` " +
                        "ON `weekly_summaries` (`weekStartMillis`)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_summary_events` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`eventType` TEXT NOT NULL, " +
                        "`targetKey` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_summary_events_timestamp` ON `weekly_summary_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_summary_events_eventType` ON `weekly_summary_events` (`eventType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_summary_events_targetKey` ON `weekly_summary_events` (`targetKey`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_summary_section_order` (" +
                        "`sectionKey` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sectionKey`))"
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `calm_personal_phrases` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`phrase` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_calm_personal_phrases_phrase` " +
                        "ON `calm_personal_phrases` (`phrase`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_calm_personal_phrases_updatedAt` " +
                        "ON `calm_personal_phrases` (`updatedAt`)"
                )
            }
        }

        fun getInstance(context: Context): NevilleRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NevilleRoomDatabase::class.java,
                    "neville_room.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18
                    )
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
