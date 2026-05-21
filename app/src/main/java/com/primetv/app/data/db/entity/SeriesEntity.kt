package com.primetv.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primetv.app.data.model.Series

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val seriesId: Int,
    val num: Int?,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdropPath: List<String>?,
    val categoryId: String?,
    val episodeRunTime: String?
) {
    fun toModel() = Series(num, name, seriesId, cover, plot, cast, director, genre, releaseDate, lastModified, rating, rating5Based, backdropPath, categoryId, episodeRunTime)
    companion object {
        fun from(s: Series) = SeriesEntity(s.seriesId, s.num, s.name, s.cover, s.plot, s.cast, s.director, s.genre, s.releaseDate, s.lastModified, s.rating, s.rating5Based, s.backdropPath, s.categoryId, s.episodeRunTime)
    }
}
