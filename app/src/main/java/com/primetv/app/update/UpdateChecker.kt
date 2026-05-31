package com.primetv.app.update

import android.util.Log
import com.primetv.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val apkUrl: String
)

object UpdateChecker {

    private const val RELEASES_URL = "https://api.github.com/repos/ivnmelendez/fantv/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun checkForUpdate(): UpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val json = JSONObject(response.body!!.string())
            val latestVersion = json.getString("tag_name").removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            if (isNewer(latestVersion, currentVersion)) {
                val assets = json.getJSONArray("assets")
                val apkUrl = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(".apk") }
                    ?.getString("browser_download_url")
                apkUrl?.let { UpdateInfo(latestVersion, it) }
            } else {
                Log.d("Update", "Already on latest: $currentVersion")
                null
            }
        } catch (e: Exception) {
            Log.e("Update", "Check failed: ${e.message}")
            null
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
