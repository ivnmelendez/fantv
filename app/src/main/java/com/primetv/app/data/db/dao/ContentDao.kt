package com.primetv.app.data.db.dao

import androidx.room.*
import com.primetv.app.data.db.entity.*

@Dao
interface ContentDao {

    // Categories
    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategories(type: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(list: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteCategories(type: String)

    // VOD
    @Query("SELECT * FROM vod_streams")
    suspend fun getVodStreams(): List<VodStreamEntity>

    @Query("SELECT * FROM vod_streams WHERE categoryId = :catId")
    suspend fun getVodStreamsByCategory(catId: String): List<VodStreamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodStreams(list: List<VodStreamEntity>)

    @Query("DELETE FROM vod_streams")
    suspend fun deleteVodStreams()

    @Query("SELECT * FROM vod_streams WHERE name LIKE '%' || :q || '%' ORDER BY added DESC LIMIT 60")
    suspend fun searchVodStreams(q: String): List<VodStreamEntity>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :q || '%' ORDER BY lastModified DESC LIMIT 60")
    suspend fun searchSeries(q: String): List<SeriesEntity>

    // Series
    @Query("SELECT * FROM series")
    suspend fun getSeries(): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE categoryId = :catId")
    suspend fun getSeriesByCategory(catId: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(list: List<SeriesEntity>)

    @Query("DELETE FROM series")
    suspend fun deleteSeries()

    // Series info cache
    @Query("SELECT * FROM series_info_cache WHERE seriesId = :seriesId LIMIT 1")
    suspend fun getSeriesInfoCache(seriesId: Int): SeriesInfoCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeriesInfoCache(entity: SeriesInfoCacheEntity)

    @Query("DELETE FROM series_info_cache WHERE cachedAt < :cutoff")
    suspend fun deleteSeriesInfoCacheOlderThan(cutoff: Long)

    @Query("""
        DELETE FROM series_info_cache
        WHERE seriesId NOT IN (
            SELECT seriesId
            FROM series_info_cache
            ORDER BY cachedAt DESC
            LIMIT :maxEntries
        )
    """)
    suspend fun pruneSeriesInfoCache(maxEntries: Int)

    // Live
    @Query("SELECT * FROM live_streams")
    suspend fun getLiveStreams(): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE categoryId = :catId")
    suspend fun getLiveStreamsByCategory(catId: String): List<LiveStreamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStreams(list: List<LiveStreamEntity>)

    @Query("DELETE FROM live_streams")
    suspend fun deleteLiveStreams()

    @Query("DELETE FROM live_streams WHERE categoryId = :catId")
    suspend fun deleteLiveStreamsByCategory(catId: String)

    // Watch history
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entity: com.primetv.app.data.db.entity.WatchHistoryEntity)

    @Query("""
        SELECT w.* FROM watch_history w
        WHERE w.positionMs > w.durationMs * 0.05
          AND w.positionMs < w.durationMs * 0.90
          AND w.watchedAt = (
              SELECT MAX(w2.watchedAt) FROM watch_history w2
              WHERE COALESCE(w2.seriesId, w2.streamId) = COALESCE(w.seriesId, w.streamId)
                AND w2.positionMs > w2.durationMs * 0.05
                AND w2.positionMs < w2.durationMs * 0.90
          )
        ORDER BY w.watchedAt DESC LIMIT 10
    """)
    suspend fun getWatchHistory(): List<com.primetv.app.data.db.entity.WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE streamId = :streamId LIMIT 1")
    suspend fun getWatchHistoryEntry(streamId: String): com.primetv.app.data.db.entity.WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE streamId = :streamId")
    suspend fun deleteWatchHistory(streamId: String)

    @Query("SELECT * FROM watch_history WHERE seriesId = :seriesId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getWatchHistoryEntryBySeriesId(seriesId: String): com.primetv.app.data.db.entity.WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE seriesId = :seriesId")
    suspend fun deleteWatchHistoryBySeriesId(seriesId: String)

    // TMDB cache
    @Query("SELECT * FROM tmdb_cache WHERE key = :key LIMIT 1")
    suspend fun getTmdbCache(key: String): TmdbCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTmdbCache(entity: TmdbCacheEntity)

    @Query("DELETE FROM tmdb_cache WHERE cachedAt < :cutoff")
    suspend fun deleteTmdbCacheOlderThan(cutoff: Long)

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getFavorites(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE streamId = :streamId LIMIT 1")
    suspend fun getFavorite(streamId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE streamId = :streamId")
    suspend fun removeFavorite(streamId: String)
}
