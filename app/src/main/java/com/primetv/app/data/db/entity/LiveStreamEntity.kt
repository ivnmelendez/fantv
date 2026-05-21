package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primetv.app.data.model.LiveStream

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val streamId: Int,
    val num: Int?,
    val name: String,
    val streamType: String?,
    val streamIcon: String?,
    val epgChannelId: String?,
    val added: String?,
    val categoryId: String?,
    val tvArchive: Int?,
    val tvArchiveDuration: Int?
) {
    fun toModel() = LiveStream(num, name, streamType, streamId, streamIcon, epgChannelId, added, categoryId, tvArchive, tvArchiveDuration)
    companion object {
        fun from(l: LiveStream) = LiveStreamEntity(l.streamId, l.num, l.name, l.streamType, l.streamIcon, l.epgChannelId, l.added, l.categoryId, l.tvArchive, l.tvArchiveDuration)
    }
}
