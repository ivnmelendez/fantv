package com.primetv.app.data.repository

import android.util.Log
import com.primetv.app.data.api.ApiClient
import com.primetv.app.data.db.AppDatabase
import com.primetv.app.data.db.entity.*
import com.primetv.app.data.model.*
import com.primetv.app.util.Prefs
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

    fun isFirstSync(): Boolean = prefs.lastVodFetchTime == 0L

    // ── Session sync ──────────────────────────────────────────────────────

    @Volatile private var syncedThisSession = false
    private val minForceSyncIntervalMs = 30 * 60 * 1000L // 30 min

    suspend fun syncSession() = coroutineScope {
        val timeSinceLastSync = System.currentTimeMillis() - prefs.lastVodFetchTime
        val forceAll = !syncedThisSession && timeSinceLastSync > minForceSyncIntervalMs
        syncedThisSession = true
        launch {
            if (forceAll || prefs.isCacheExpired(prefs.lastVodFetchTime))
                runCatching { syncVod() }.onFailure { Log.e("Sync", "VOD sync failed: ${it.message}") }
        }
        launch {
            if (forceAll || prefs.isCacheExpired(prefs.lastSeriesFetchTime))
                runCatching { syncSeries() }.onFailure { Log.e("Sync", "Series sync failed: ${it.message}") }
        }
        launch {
            if (forceAll || prefs.isCacheExpired(prefs.lastLiveFetchTime))
                runCatching { syncLive() }.onFailure { Log.e("Sync", "Live sync failed: ${it.message}") }
        }
    }

    private suspend fun syncVod() {
        Log.d("Sync", "VOD sync start")
        val cats = api.getVodCategories(apiUrl(), user(), pass())
        dao.deleteCategories("vod")
        dao.insertCategories(cats.mapNotNull { CategoryEntity.from(it, "vod") })
        val streams = api.getVodStreams(apiUrl(), user(), pass())
        dao.deleteVodStreams()
        dao.insertVodStreams(streams.mapNotNull { runCatching { VodStreamEntity.from(it) }.getOrNull() })
        prefs.lastVodFetchTime = System.currentTimeMillis()
        Log.d("Sync", "VOD sync done: ${cats.size} cats, ${streams.size} streams")
    }

    private suspend fun syncSeries() {
        Log.d("Sync", "Series sync start")
        val cats = api.getSeriesCategories(apiUrl(), user(), pass())
        dao.deleteCategories("series")
        dao.insertCategories(cats.mapNotNull { CategoryEntity.from(it, "series") })
        val series = api.getSeries(apiUrl(), user(), pass())
        dao.deleteSeries()
        dao.insertSeries(series.mapNotNull { runCatching { SeriesEntity.from(it) }.getOrNull() })
        prefs.lastSeriesFetchTime = System.currentTimeMillis()
        Log.d("Sync", "Series sync done: ${cats.size} cats, ${series.size} series")
    }

    private suspend fun syncLive() {
        Log.d("Sync", "Live sync start")
        val cats = api.getLiveCategories(apiUrl(), user(), pass())
        dao.deleteCategories("live")
        dao.insertCategories(cats.mapNotNull { CategoryEntity.from(it, "live") })
        val streams = api.getLiveStreams(apiUrl(), user(), pass())
        dao.deleteLiveStreams()
        dao.insertLiveStreams(streams.map { LiveStreamEntity.from(it) })
        prefs.lastLiveFetchTime = System.currentTimeMillis()
        Log.d("Sync", "Live sync done: ${cats.size} cats, ${streams.size} streams")
    }

    // ── Stream URLs ───────────────────────────────────────────────────────

    fun buildVodUrl(streamId: Int, ext: String) =
        ApiClient.vodStreamUrl(prefs.normalizedServer(), user(), pass(), streamId, ext)

    fun buildLiveUrl(streamId: Int) =
        ApiClient.liveStreamUrl(prefs.normalizedServer(), user(), pass(), streamId)

    fun buildSeriesUrl(episodeId: String, ext: String) =
        ApiClient.seriesStreamUrl(prefs.normalizedServer(), user(), pass(), episodeId, ext)
}
