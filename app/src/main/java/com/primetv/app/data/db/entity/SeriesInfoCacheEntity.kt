package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "series_info_cache")
data class SeriesInfoCacheEntity(
    @PrimaryKey val seriesId: Int,
    val payloadJson: String,
    val cachedAt: Long
)
