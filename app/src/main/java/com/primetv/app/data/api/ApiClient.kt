package com.primetv.app.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapterFactory(FlexibleListAdapterFactory())
            .create()
    }

    /** Retrofit with a dummy base URL — actual URLs are passed per-call via @Url */
    val api: XtreamApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(XtreamApi::class.java)
    }

    /** Build the player_api.php base URL for a given server */
    fun apiUrl(serverUrl: String) = "${serverUrl.trimEnd('/')}/player_api.php"

    /** Build VOD stream URL */
    fun vodStreamUrl(server: String, user: String, pass: String, streamId: Int, ext: String) =
        "${server.trimEnd('/')}/movie/$user/$pass/$streamId.$ext"

    /** Build live stream URL (HLS) */
    fun liveStreamUrl(server: String, user: String, pass: String, streamId: Int) =
        "${server.trimEnd('/')}/live/$user/$pass/$streamId.m3u8"

    /** Build series episode URL */
    fun seriesStreamUrl(server: String, user: String, pass: String, episodeId: String, ext: String) =
        "${server.trimEnd('/')}/series/$user/$pass/$episodeId.$ext"
}
