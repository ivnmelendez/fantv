package com.primetv.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.primetv.app.data.db.dao.ContentDao
import com.primetv.app.data.db.entity.*

@Database(
    entities = [CategoryEntity::class, VodStreamEntity::class, SeriesEntity::class, LiveStreamEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "primetv.db")
                .build().also { instance = it }
        }
    }
}
