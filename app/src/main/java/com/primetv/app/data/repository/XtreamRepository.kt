package com.primetv.app.data.repository

import com.primetv.app.data.api.ApiClient
import com.primetv.app.data.db.AppDatabase
import com.primetv.app.data.db.entity.*
import com.primetv.app.data.model.*
import com.primetv.app.util.Prefs

class XtreamRepository(private val prefs: Prefs, private val db: AppDatabase) {

    private val api = ApiClient.api
    private val dao = db.contentDao()

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
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastVodFetchTime)) return cached.map { it.toModel() }
        return api.getVodCategories(apiUrl(), user(), pass()).also { list ->
            dao.deleteCategories("vod")
            dao.insertCategories(list.mapNotNull { CategoryEntity.from(it, "vod") })
            prefs.lastVodFetchTime = System.currentTimeMillis()
        }
    }

    suspend fun getVodStreams(categoryId: String? = null): List<VodStream> {
        val cached = if (categoryId != null) dao.getVodStreamsByCategory(categoryId) else dao.getVodStreams()
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastVodFetchTime)) return cached.map { it.toModel() }
        return api.getVodStreams(apiUrl(), user(), pass(), categoryId = categoryId).also { list ->
            if (categoryId == null) dao.deleteVodStreams()
            dao.insertVodStreams(list.mapNotNull { runCatching { VodStreamEntity.from(it) }.getOrNull() })
            prefs.lastVodFetchTime = System.currentTimeMillis()
        }
    }

    suspend fun getVodInfo(vodId: Int): VodInfoResponse =
        api.getVodInfo(apiUrl(), user(), pass(), vodId = vodId)

    // ── Series ────────────────────────────────────────────────────────────

    suspend fun getSeriesCategories(): List<Category> {
        val cached = dao.getCategories("series")
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastSeriesFetchTime)) return cached.map { it.toModel() }
        return api.getSeriesCategories(apiUrl(), user(), pass()).also { list ->
            dao.deleteCategories("series")
            dao.insertCategories(list.mapNotNull { CategoryEntity.from(it, "series") })
            prefs.lastSeriesFetchTime = System.currentTimeMillis()
        }
    }

    suspend fun getSeries(categoryId: String? = null): List<Series> {
        val cached = if (categoryId != null) dao.getSeriesByCategory(categoryId) else dao.getSeries()
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastSeriesFetchTime)) return cached.map { it.toModel() }
        return api.getSeries(apiUrl(), user(), pass(), categoryId = categoryId).also { list ->
            if (categoryId == null) dao.deleteSeries()
            dao.insertSeries(list.mapNotNull { runCatching { SeriesEntity.from(it) }.getOrNull() })
            prefs.lastSeriesFetchTime = System.currentTimeMillis()
        }
    }

    suspend fun getSeriesInfo(seriesId: Int): SeriesInfoResponse =
        api.getSeriesInfo(apiUrl(), user(), pass(), seriesId = seriesId)

    // ── Live ──────────────────────────────────────────────────────────────

    suspend fun getLiveCategories(): List<Category> {
        val cached = dao.getCategories("live")
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastLiveFetchTime)) return cached.map { it.toModel() }
        return api.getLiveCategories(apiUrl(), user(), pass()).also { list ->
            dao.deleteCategories("live")
            dao.insertCategories(list.mapNotNull { CategoryEntity.from(it, "live") })
            prefs.lastLiveFetchTime = System.currentTimeMillis()
        }
    }

    suspend fun getLiveStreams(categoryId: String? = null): List<LiveStream> {
        val cached = if (categoryId != null) dao.getLiveStreamsByCategory(categoryId) else dao.getLiveStreams()
        if (cached.isNotEmpty() && !prefs.isCacheExpired(prefs.lastLiveFetchTime)) return cached.map { it.toModel() }
        return api.getLiveStreams(apiUrl(), user(), pass(), categoryId = categoryId).also { list ->
            if (categoryId == null) dao.deleteLiveStreams()
            dao.insertLiveStreams(list.mapNotNull { runCatching { LiveStreamEntity.from(it) }.getOrNull() })
            prefs.lastLiveFetchTime = System.currentTimeMillis()
        }
    }

    // ── Stream URLs ───────────────────────────────────────────────────────

    fun buildVodUrl(streamId: Int, ext: String) =
        ApiClient.vodStreamUrl(prefs.normalizedServer(), user(), pass(), streamId, ext)

    fun buildLiveUrl(streamId: Int) =
        ApiClient.liveStreamUrl(prefs.normalizedServer(), user(), pass(), streamId)

    fun buildSeriesUrl(episodeId: String, ext: String) =
        ApiClient.seriesStreamUrl(prefs.normalizedServer(), user(), pass(), episodeId, ext)
}
