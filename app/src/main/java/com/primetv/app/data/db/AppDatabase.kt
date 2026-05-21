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
    entities = [CategoryEntity::class, VodStreamEntity::class, SeriesEntity::class, LiveStreamEntity::class, SeriesInfoCacheEntity::class],
    version = 2,
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

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "primetv.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
