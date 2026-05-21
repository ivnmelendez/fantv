package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primetv.app.data.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: Int,
    val type: String  // "vod" | "series" | "live"
) {
    fun toModel() = Category(id, name, parentId)
    companion object {
        fun from(c: Category, type: String): CategoryEntity? {
            val id = c.id?.takeIf { it.isNotBlank() } ?: return null
            val name = c.name?.takeIf { it.isNotBlank() } ?: return null
            return CategoryEntity(id, name, c.parentId, type)
        }
    }
}
