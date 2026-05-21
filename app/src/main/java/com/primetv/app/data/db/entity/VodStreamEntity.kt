package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primetv.app.data.model.VodStream

@Entity(tableName = "vod_streams")
data class VodStreamEntity(
    @PrimaryKey val streamId: Int,
    val num: Int?,
    val name: String,
    val streamIcon: String?,
    val rating: String?,
    val rating5Based: Double?,
    val added: String?,
    val categoryId: String?,
    val containerExtension: String?,
    val customSid: String?,
    val directSource: String?
) {
    fun toModel() = VodStream(num, name, streamId, streamIcon, rating, rating5Based, added, categoryId, containerExtension, customSid, directSource)
    companion object {
        fun from(v: VodStream) = VodStreamEntity(v.streamId, v.num, v.name, v.streamIcon, v.rating, v.rating5Based, v.added, v.categoryId, v.containerExtension, v.customSid, v.directSource)
    }
}
