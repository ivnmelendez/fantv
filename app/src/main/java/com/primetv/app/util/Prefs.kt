package com.primetv.app.util

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("prime_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_SERVER, v).apply()

    var username: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_USER, v).apply()

    var password: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        set(v) = prefs.edit().putString(KEY_PASS, v).apply()

    var activeMenuIndex: Int
        get() = prefs.getInt(KEY_MENU_INDEX, 0)
        set(v) = prefs.edit().putInt(KEY_MENU_INDEX, v).apply()

    var lastFetchTime: Long
        get() = prefs.getLong(KEY_LAST_FETCH, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_FETCH, v).apply()

    var lastVodFetchTime: Long
        get() = prefs.getLong(KEY_LAST_VOD_FETCH, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_VOD_FETCH, v).apply()

    var lastSeriesFetchTime: Long
        get() = prefs.getLong(KEY_LAST_SERIES_FETCH, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_SERIES_FETCH, v).apply()

    var lastLiveFetchTime: Long
        get() = prefs.getLong(KEY_LAST_LIVE_FETCH, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_LIVE_FETCH, v).apply()

    fun isCacheExpired(lastFetch: Long): Boolean {
        val elapsed = System.currentTimeMillis() - lastFetch
        return elapsed > 24 * 60 * 60 * 1000L
    }

    fun hasCredentials() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun clear() = prefs.edit().clear().apply()

    /** Normalize server URL — strip trailing slash, ensure no double-slash in paths */
    fun normalizedServer(): String = serverUrl.trimEnd('/')

    companion object {
        private const val KEY_SERVER = "server_url"
        private const val KEY_USER = "username"
        private const val KEY_PASS = "password"
        private const val KEY_MENU_INDEX = "menu_index"
        private const val KEY_LAST_FETCH = "last_fetch_time"
        private const val KEY_LAST_VOD_FETCH = "last_vod_fetch_time"
        private const val KEY_LAST_SERIES_FETCH = "last_series_fetch_time"
        private const val KEY_LAST_LIVE_FETCH = "last_live_fetch_time"
    }
}
