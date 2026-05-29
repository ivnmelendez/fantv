package com.primetv.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.primetv.app.data.db.dao.ContentDao
import com.primetv.app.data.db.entity.*
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, VodStreamEntity::class, SeriesEntity::class, LiveStreamEntity::class, SeriesInfoCacheEntity::class, TmdbCacheEntity::class, com.primetv.app.data.db.entity.WatchHistoryEntity::class, com.primetv.app.data.db.entity.FavoriteEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS series_info_cache (
                        seriesId INTEGER NOT NULL PRIMARY KEY,
                        payloadJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tmdb_cache (key TEXT NOT NULL PRIMARY KEY, payloadJson TEXT NOT NULL, cachedAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_history (
                        streamId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        posterUrl TEXT,
                        streamUrl TEXT NOT NULL,
                        ext TEXT NOT NULL,
                        positionMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        isSeries INTEGER NOT NULL,
                        watchedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_history ADD COLUMN seriesId TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        streamId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        posterUrl TEXT,
                        isSeries INTEGER NOT NULL,
                        ext TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "primetv.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { instance = it }
        }
    }
}
