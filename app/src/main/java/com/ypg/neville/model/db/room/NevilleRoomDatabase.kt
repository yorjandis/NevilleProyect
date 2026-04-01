package com.ypg.neville.model.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotaEntity::class, FraseEntity::class, ConfEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NevilleRoomDatabase : RoomDatabase() {

    abstract fun notaDao(): NotaDao
    abstract fun fraseDao(): FraseDao
    abstract fun confDao(): ConfDao

    companion object {
        @Volatile
        private var INSTANCE: NevilleRoomDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
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
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_frases_frase` ON `frases` (`frase`)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `repo` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repo_title` ON `repo` (`title`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repo_link` ON `repo` (`link`)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `videos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_videos_title` ON `videos` (`title`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_videos_link` ON `videos` (`link`)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `conf` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`link` TEXT NOT NULL, " +
                        "`fav` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL, " +
                        "`shared` TEXT NOT NULL)"
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_conf_title` ON `conf` (`title`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_conf_link` ON `conf` (`link`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `videos`")
                database.execSQL("DROP TABLE IF EXISTS `repo`")
            }
        }

        fun getInstance(context: Context): NevilleRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NevilleRoomDatabase::class.java,
                    "neville_room.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
