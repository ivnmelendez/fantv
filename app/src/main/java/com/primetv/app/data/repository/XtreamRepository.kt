package com.primetv.app.data.repository

import com.primetv.app.data.api.ApiClient
import com.primetv.app.data.db.AppDatabase
import com.primetv.app.data.db.entity.*
import com.primetv.app.data.model.*
import com.primetv.app.util.Prefs
import com.google.gson.Gson

class XtreamRepository(private val prefs: Prefs, private val db: AppDatabase) {

    private val api = ApiClient.api
    private val dao = db.contentDao()
    private val gson = Gson()
    private val seriesInfoCacheMaxEntries = 200
    private val seriesInfoCacheTtlMs = 7L * 24 * 60 * 60 * 1000

    private fun apiUrl() = ApiClient.apiUrl(prefs.normalizedServer())
    private fun user() = prefs.username
    private fun pass() = prefs.password

    // ── Auth ──────────────────────────────────────────────────────────────

    suspend fun authenticate(server: String, username: String, password: String): AuthResponse {
        val url = ApiClient.apiUrl(server)
        return api.authenticate(url, username, password)
    }

    // ── VOD ───────────────────────────────────────────────────────────────

    suspend fun getVodCategories(): List<Category> {
        val cached = dao.getCategories("vod")
        return cached.map { it.toModel() }
    }

    suspend fun getVodStreams(categoryId: String? = null): List<VodStream> {
        val cached = if (categoryId != null) dao.getVodStreamsByCategory(categoryId) else dao.getVodStreams()
        return cached.map { it.toModel() }
    }

    suspend fun getVodInfo(vodId: Int): VodInfoResponse =
        api.getVodInfo(apiUrl(), user(), pass(), vodId = vodId)

    // ── Series ────────────────────────────────────────────────────────────

    suspend fun getSeriesCategories(): List<Category> {
        val cached = dao.getCategories("series")
        return cached.map { it.toModel() }
    }

    suspend fun getSeries(categoryId: String? = null): List<Series> {
        val cached = if (categoryId != null) dao.getSeriesByCategory(categoryId) else dao.getSeries()
        return cached.map { it.toModel() }
    }

    suspend fun getSeriesInfo(seriesId: Int): SeriesInfoResponse =
        dao.getSeriesInfoCache(seriesId)?.let { cached ->
            runCatching {
                gson.fromJson(cached.payloadJson, SeriesInfoResponse::class.java)
            }.getOrNull()?.let { return it }
        } ?: api.getSeriesInfo(apiUrl(), user(), pass(), seriesId = seriesId).also { response ->
            runCatching {
                dao.upsertSeriesInfoCache(
                    SeriesInfoCacheEntity(
                        seriesId = seriesId,
                        payloadJson = gson.toJson(response),
                        cachedAt = System.currentTimeMillis()
                    )
                )
                dao.deleteSeriesInfoCacheOlderThan(System.currentTimeMillis() - seriesInfoCacheTtlMs)
                dao.pruneSeriesInfoCache(seriesInfoCacheMaxEntries)
            }
        }

    // ── Live ──────────────────────────────────────────────────────────────

    suspend fun getLiveCategories(): List<Category> {
        val cached = dao.getCategories("live")
        return cached.map { it.toModel() }
    }

    suspend fun getLiveStreams(categoryId: String? = null): List<LiveStream> {
        val cached = if (categoryId != null) dao.getLiveStreamsByCategory(categoryId) else dao.getLiveStreams()
        return cached.map { it.toModel() }
    }

    suspend fun fetchLiveCategoriesDirect(): List<Category> =
        api.getLiveCategories(apiUrl(), user(), pass())

    suspend fun fetchLiveStreamsDirect(categoryId: String): List<LiveStream> =
        api.getLiveStreams(apiUrl(), user(), pass(), categoryId = categoryId)

    suspend fun refreshLiveCategory(categoryId: String, streams: List<LiveStream>) {
        dao.deleteLiveStreamsByCategory(categoryId)
        dao.insertLiveStreams(streams.map { LiveStreamEntity.from(it) })
    }

    // ── Stream URLs ───────────────────────────────────────────────────────

    fun buildVodUrl(streamId: Int, ext: String) =
        ApiClient.vodStreamUrl(prefs.normalizedServer(), user(), pass(), streamId, ext)

    fun buildLiveUrl(streamId: Int) =
        ApiClient.liveStreamUrl(prefs.normalizedServer(), user(), pass(), streamId)

    fun buildSeriesUrl(episodeId: String, ext: String) =
        ApiClient.seriesStreamUrl(prefs.normalizedServer(), user(), pass(), episodeId, ext)
}
