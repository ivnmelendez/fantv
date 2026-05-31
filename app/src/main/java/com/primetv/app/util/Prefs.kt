package com.primetv.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("prime_prefs", Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "prime_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { migratePlainToSecure(it) }
    }

    private fun migratePlainToSecure(secure: SharedPreferences) {
        val plainServer = prefs.getString(KEY_SERVER, null)
        val plainUser   = prefs.getString(KEY_USER, null)
        val plainPass   = prefs.getString(KEY_PASS, null)
        if (plainServer != null || plainUser != null || plainPass != null) {
            secure.edit().apply {
                if (plainServer != null) putString(KEY_SERVER, plainServer)
                if (plainUser   != null) putString(KEY_USER, plainUser)
                if (plainPass   != null) putString(KEY_PASS, plainPass)
            }.apply()
            prefs.edit().remove(KEY_SERVER).remove(KEY_USER).remove(KEY_PASS).apply()
        }
    }

    var serverUrl: String
        get() = securePrefs.getString(KEY_SERVER, "") ?: ""
        set(v) = securePrefs.edit().putString(KEY_SERVER, v).apply()

    var username: String
        get() = securePrefs.getString(KEY_USER, "") ?: ""
        set(v) = securePrefs.edit().putString(KEY_USER, v).apply()

    var password: String
        get() = securePrefs.getString(KEY_PASS, "") ?: ""
        set(v) = securePrefs.edit().putString(KEY_PASS, v).apply()

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

    var lastLiveStreamId: Int
        get() = prefs.getInt(KEY_LAST_LIVE_STREAM, -1)
        set(v) = prefs.edit().putInt(KEY_LAST_LIVE_STREAM, v).apply()

    fun isCacheExpired(lastFetch: Long): Boolean {
        val elapsed = System.currentTimeMillis() - lastFetch
        return elapsed > 24 * 60 * 60 * 1000L
    }

    fun hasCredentials() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
    }

    fun normalizedServer(): String = serverUrl.trimEnd('/')

    companion object {
        private const val KEY_SERVER       = "server_url"
        private const val KEY_USER         = "username"
        private const val KEY_PASS         = "password"
        private const val KEY_MENU_INDEX   = "menu_index"
        private const val KEY_LAST_FETCH   = "last_fetch_time"
        private const val KEY_LAST_VOD_FETCH    = "last_vod_fetch_time"
        private const val KEY_LAST_SERIES_FETCH = "last_series_fetch_time"
        private const val KEY_LAST_LIVE_FETCH   = "last_live_fetch_time"
        private const val KEY_LAST_LIVE_STREAM  = "last_live_stream_id"
    }
}
