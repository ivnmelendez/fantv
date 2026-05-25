package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val streamId: String,
    val title: String,
    val posterUrl: String?,
    val streamUrl: String,
    val ext: String,
    val positionMs: Long,
    val durationMs: Long,
    val isSeries: Boolean,
    val watchedAt: Long
)
