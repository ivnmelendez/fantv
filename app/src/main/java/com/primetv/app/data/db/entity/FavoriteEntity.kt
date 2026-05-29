package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val streamId: String,
    val title: String,
    val posterUrl: String?,
    val isSeries: Boolean,
    val ext: String,
    val addedAt: Long = System.currentTimeMillis()
)
