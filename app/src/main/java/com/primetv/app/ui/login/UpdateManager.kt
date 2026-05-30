package com.primetv.app.ui.login

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.primetv.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val activity: LoginActivity) {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/ivnmelendez/fantv/releases/latest"
        private const val APK_ASSET_NAME = "fantv.apk"
    }

    private var pendingApkUrl: String? = null

    fun check(onNoUpdate: () -> Unit) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = fetchLatestRelease()
                withContext(Dispatchers.Main) {
                    if (result != null && result.first > BuildConfig.VERSION_CODE) {
                        pendingApkUrl = result.second
                        showMandatoryDialog(result.second)
                    } else {
                        onNoUpdate()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onNoUpdate() }
            }
        }
    }

    fun recheckIfPending() {
        val url = pendingApkUrl ?: return
        showMandatoryDialog(url)
    }

    private fun fetchLatestRelease(): Pair<Int, String>? {
        val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        return try {
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val versionCode = json.getString("tag_name").removePrefix("v").toIntOrNull() ?: return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == APK_ASSET_NAME) {
                    return Pair(versionCode, asset.getString("browser_download_url"))
                }
            }
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun showMandatoryDialog(apkUrl: String) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Actualización requerida")
            .setMessage("Hay una nueva versión disponible. Debes actualizar para continuar usando FanTV.")
            .setCancelable(false)
            .setPositiveButton("Actualizar", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(Color.WHITE)
            setOnClickListener {
                dialog.dismiss()
                startDownload(apkUrl)
            }
        }
    }

    private fun startDownload(apkUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            )
            return
        }

        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("Descargando actualización…")
            .setMessage("0%")
            .setCancelable(false)
            .show()

        val apkFile = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            APK_ASSET_NAME
        )
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("FanTV - Actualizando")
            setDestinationUri(Uri.fromFile(apkFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        }
        val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            var running = true
            while (running) {
                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    withContext(Dispatchers.Main) {
                        if (total > 0) progressDialog.setMessage("${downloaded * 100 / total}%")
                    }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            running = false
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                installApk(apkFile)
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            running = false
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                showMandatoryDialog(apkUrl)
                            }
                        }
                    }
                }
                cursor.close()
                delay(500)
            }
        }
    }

    private fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
