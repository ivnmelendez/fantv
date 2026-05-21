package com.primetv.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class TmdbSearchResponse(
    val results: List<TmdbResult>?
)

data class TmdbResult(
    val id: Int?,
    val title: String?,
    val name: String?,
    val overview: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genre_ids") val genreIds: List<Int>?
)

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse
}
