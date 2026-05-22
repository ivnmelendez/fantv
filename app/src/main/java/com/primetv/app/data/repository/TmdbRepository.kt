package com.primetv.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.primetv.app.data.api.TmdbApi
import com.primetv.app.data.api.TmdbResult
import com.primetv.app.data.db.dao.ContentDao
import com.primetv.app.data.db.entity.TmdbCacheEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TmdbRepository(private val dao: ContentDao) {

    companion object {
        private const val API_KEY = "f010590c285895a245fc704377c7f398"
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000
        private const val SEARCH_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        const val IMAGE_W1280 = "https://image.tmdb.org/t/p/w1280"

        private val QUALITY_REGEX = Regex(
            """\(?(HDTS|DCPRIP|DC-RIP|HDCAM|HDSCR|DVDScr|CAM|SCR|TS)\)?""",
            RegexOption.IGNORE_CASE
        )

        fun extractQualityTag(title: String): String? =
            QUALITY_REGEX.find(title)?.value?.trim('(', ')', ' ')?.uppercase()

        // Strip year from END only — preserves titles like "2001: A Space Odyssey"
        private val YEAR_SUFFIX_REGEX = Regex("""\s*\(?(19|20)\d{2}\)?\s*$""")

        fun normalizeTitle(raw: String): String = raw
            .replace(QUALITY_REGEX, "")
            .replace(YEAR_SUFFIX_REGEX, "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .lowercase()

        fun extractYear(raw: String): String? =
            Regex("""\b(19|20)\d{2}\b""").find(raw)?.value

        private val MOVIE_GENRES = mapOf(
            28 to "Acción", 12 to "Aventura", 16 to "Animación", 35 to "Comedia",
            80 to "Crimen", 99 to "Documental", 18 to "Drama", 10751 to "Familia",
            14 to "Fantasía", 36 to "Historia", 27 to "Terror", 10402 to "Música",
            9648 to "Misterio", 10749 to "Romance", 878 to "Ciencia ficción",
            53 to "Suspenso", 10752 to "Bélica", 37 to "Western"
        )
        private val TV_GENRES = mapOf(
            10759 to "Acción", 16 to "Animación", 35 to "Comedia", 80 to "Crimen",
            99 to "Documental", 18 to "Drama", 10751 to "Familia", 9648 to "Misterio",
            10765 to "Ciencia ficción", 37 to "Western"
        )
    }

    private val api: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(trustAllClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    private fun trustAllClient(): OkHttpClient {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val ssl = SSLContext.getInstance("SSL").apply { init(null, trustAll, SecureRandom()) }
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private val searchCache = HashMap<String, TmdbResult>()
    private val gson = Gson()
    private val tmdbListType = object : TypeToken<List<TmdbResult>>() {}.type

    private suspend fun cachedFetch(key: String, fetcher: suspend () -> List<TmdbResult>): List<TmdbResult> {
        val now = System.currentTimeMillis()
        val cached = dao.getTmdbCache(key)
        if (cached != null && now - cached.cachedAt < CACHE_TTL_MS) {
            Log.d("TMDB", "cache HIT: $key (age ${(now - cached.cachedAt) / 3600000}h)")
            return gson.fromJson(cached.payloadJson, tmdbListType)
        }
        Log.d("TMDB", "cache MISS: $key — fetching API")
        val results = fetcher()
        if (results.isNotEmpty()) {
            dao.upsertTmdbCache(TmdbCacheEntity(key, gson.toJson(results), now))
            dao.deleteTmdbCacheOlderThan(now - CACHE_TTL_MS * 7)
        }
        return results
    }

    suspend fun search(title: String, isSeries: Boolean): TmdbResult? {
        val query = cleanTitle(title)
        val dbKey = "search:$query:$isSeries"

        searchCache[dbKey]?.let { return it }

        val now = System.currentTimeMillis()
        val cached = dao.getTmdbCache(dbKey)
        if (cached != null && now - cached.cachedAt < SEARCH_CACHE_TTL_MS) {
            Log.d("TMDB", "search cache HIT: '$query'")
            val result = gson.fromJson(cached.payloadJson, TmdbResult::class.java)
            searchCache[dbKey] = result
            return result
        }

        Log.d("TMDB", "search API: '$query' isSeries=$isSeries")
        return try {
            val result = if (isSeries) api.searchTv(API_KEY, query).results?.firstOrNull()
                         else api.searchMovie(API_KEY, query).results?.firstOrNull()
            Log.d("TMDB", "result: ${result?.title ?: result?.name} backdrop=${result?.backdropPath}")
            result?.also {
                searchCache[dbKey] = it
                dao.upsertTmdbCache(TmdbCacheEntity(dbKey, gson.toJson(it), now))
            }
        } catch (e: Exception) {
            Log.e("TMDB", "search failed: ${e.message}")
            null
        }
    }

    fun backdropUrl(path: String?): String? =
        if (!path.isNullOrBlank()) "$IMAGE_W1280$path" else null

    fun genreNames(ids: List<Int>?, isSeries: Boolean): String {
        val map = if (isSeries) TV_GENRES else MOVIE_GENRES
        return ids?.mapNotNull { map[it] }?.take(3)?.joinToString(", ") ?: ""
    }

    private fun cleanTitle(raw: String) = normalizeTitle(raw)

    suspend fun fetchTrendingMovies(): List<TmdbResult> = cachedFetch("trending_movies") {
        coroutineScope {
            val p1 = async { runCatching { api.trendingMovies(API_KEY, page = 1).results ?: emptyList() }.getOrElse { emptyList() } }
            val p2 = async { runCatching { api.trendingMovies(API_KEY, page = 2).results ?: emptyList() }.getOrElse { emptyList() } }
            (p1.await() + p2.await()).distinctBy { it.id }
        }
    }

    suspend fun fetchTrendingTv(): List<TmdbResult> = cachedFetch("trending_tv") {
        coroutineScope {
            val p1 = async { runCatching { api.trendingTv(API_KEY, page = 1).results ?: emptyList() }.getOrElse { emptyList() } }
            val p2 = async { runCatching { api.trendingTv(API_KEY, page = 2).results ?: emptyList() }.getOrElse { emptyList() } }
            (p1.await() + p2.await()).distinctBy { it.id }
        }
    }

    suspend fun fetchNowPlayingMovies(): List<TmdbResult> = cachedFetch("now_playing_movies") {
        coroutineScope {
            val p1 = async { runCatching { api.nowPlayingMovies(API_KEY, page = 1).results ?: emptyList() }.getOrElse { emptyList() } }
            val p2 = async { runCatching { api.nowPlayingMovies(API_KEY, page = 2).results ?: emptyList() }.getOrElse { emptyList() } }
            (p1.await() + p2.await()).distinctBy { it.id }
        }
    }
}
