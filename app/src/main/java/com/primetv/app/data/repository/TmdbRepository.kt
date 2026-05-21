package com.primetv.app.data.repository

import com.primetv.app.data.api.TmdbApi
import com.primetv.app.data.api.TmdbResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    suspend fun search(title: String, isSeries: Boolean): TmdbResult? {
        val query = cleanTitle(title)
        return try {
            if (isSeries) api.searchTv(API_KEY, query).results?.firstOrNull()
            else api.searchMovie(API_KEY, query).results?.firstOrNull()
        } catch (_: Exception) { null }
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
