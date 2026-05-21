package com.primetv.app.data.repository

import android.util.Log
import com.primetv.app.data.api.TmdbApi
import com.primetv.app.data.api.TmdbResult
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

class TmdbRepository {

    companion object {
        private const val API_KEY = "f010590c285895a245fc704377c7f398"
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_W1280 = "https://image.tmdb.org/t/p/w1280"

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

    private val cache = HashMap<String, TmdbResult>()

    suspend fun search(title: String, isSeries: Boolean): TmdbResult? {
        val query = cleanTitle(title)
        val key = "$query:$isSeries"
        cache[key]?.let { return it }
        Log.d("TMDB", "search: '$query' isSeries=$isSeries")
        return try {
            val result = if (isSeries) api.searchTv(API_KEY, query).results?.firstOrNull()
                         else api.searchMovie(API_KEY, query).results?.firstOrNull()
            Log.d("TMDB", "result: ${result?.title ?: result?.name} backdrop=${result?.backdropPath}")
            result?.also { cache[key] = it }
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

    private fun cleanTitle(raw: String) = raw
        .replace(Regex("\\(\\d{4}\\)"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}
