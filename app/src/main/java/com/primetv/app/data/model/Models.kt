package com.primetv.app.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class AuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    val username: String?,
    val password: String?,
    val auth: Int?,
    val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("is_trial") val isTrial: String?,
    @SerializedName("active_cons") val activeConnections: String?,
    @SerializedName("max_connections") val maxConnections: String?,
    @SerializedName("allowed_output_formats") val outputFormats: List<String>?
)

data class ServerInfo(
    val url: String?,
    val port: String?,
    @SerializedName("https_port") val httpsPort: String?,
    @SerializedName("server_protocol") val protocol: String?,
    val timezone: String?,
    @SerializedName("time_now") val timeNow: String?
)

// ─── Category ─────────────────────────────────────────────────────────────────

data class Category(
    @SerializedName("category_id") val id: String?,
    @SerializedName("category_name") val name: String?,
    @SerializedName("parent_id") val parentId: Int = 0
)

// ─── VOD ──────────────────────────────────────────────────────────────────────

data class VodStream(
    val num: Int?,
    val name: String,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String?,
    val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("direct_source") val directSource: String?
)

data class VodInfoResponse(
    val info: VodInfo?,
    @SerializedName("movie_data") val movieData: VodMovieData?
)

data class VodInfo(
    val name: String?,
    @SerializedName("cover_big") val coverBig: String?,
    @SerializedName("movie_image") val movieImage: String?,
    val releasedate: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    val director: String?,
    val actors: String?,
    val cast: String?,
    val description: String?,
    val plot: String?,
    val age: String?,
    @SerializedName("mpaa_rating") val mpaaRating: String?,
    val country: String?,
    val genre: String?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("duration_secs") val durationSecs: Long?,
    val duration: String?,
    val rating: Double?,
    val subtitles: List<String>?
)

data class VodMovieData(
    @SerializedName("stream_id") val streamId: Int?,
    val name: String?,
    @SerializedName("container_extension") val containerExtension: String?
)

// ─── Series ───────────────────────────────────────────────────────────────────

data class Series(
    val num: Int?,
    val name: String,
    @SerializedName("series_id") val seriesId: Int,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    @SerializedName("last_modified") val lastModified: String?,
    val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?
)

data class SeriesInfoResponse(
    val seasons: List<Season>?,
    val info: SeriesInfo?,
    val episodes: Map<String, List<Episode>>?
)

data class Season(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    val id: Int?,
    val name: String?,
    val overview: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    val cover: String?,
    @SerializedName("cover_big") val coverBig: String?
)

data class SeriesInfo(
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class Episode(
    val id: String?,
    @SerializedName("episode_num") val episodeNum: Int,
    val title: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    val info: EpisodeInfo?,
    val added: String?,
    val season: Int
)

data class EpisodeInfo(
    @SerializedName("movie_image") val movieImage: String?,
    val plot: String?,
    val rating: String?,
    val season: Int?,
    @SerializedName("episode_num") val episodeNum: Int?,
    @SerializedName("duration_secs") val durationSecs: Long?,
    val duration: String?
)

// ─── Live ─────────────────────────────────────────────────────────────────────

data class LiveStream(
    val num: Int?,
    val name: String,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("tv_archive") val tvArchive: Int?,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: Int?
)

// ─── UI model ─────────────────────────────────────────────────────────────────

data class MenuItem(
    val id: Int,
    val title: String,
    val iconResId: Int
)
