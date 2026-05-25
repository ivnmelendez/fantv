package com.primetv.app.ui.player

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.primetv.app.App
import com.primetv.app.data.db.entity.WatchHistoryEntity
import com.primetv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    companion object {
        const val EXTRA_URL       = "stream_url"
        const val EXTRA_TITLE     = "title"
        const val EXTRA_STREAM_ID = "stream_id"
        const val EXTRA_POSTER    = "poster_url"
        const val EXTRA_EXT       = "stream_ext"
        const val EXTRA_POSITION  = "start_position_ms"
        const val EXTRA_IS_SERIES = "is_series"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url      = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val position = intent.getLongExtra(EXTRA_POSITION, 0L)

        initPlayer(url, position)
    }

    private fun initPlayer(url: String, startPositionMs: Long = 0L) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.pbBuffer.visibility = when (state) {
                        Player.STATE_BUFFERING -> View.VISIBLE
                        else -> View.GONE
                    }
                    if (state == Player.STATE_ENDED) {
                        deleteWatchHistory()
                        finish()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    binding.pbBuffer.visibility = View.GONE
                    binding.rlayError.visibility = View.VISIBLE
                    binding.tvError.text = "Playback error: ${error.message}"
                }
            })

            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            if (startPositionMs > 0L) exo.seekTo(startPositionMs)
            exo.playWhenReady = true
        }
    }

    private fun saveWatchHistory() {
        val streamId = intent.getStringExtra(EXTRA_STREAM_ID) ?: return
        val exo = player ?: return
        val position = exo.currentPosition
        val duration = exo.duration.takeIf { it > 0 } ?: return
        if (position < 60_000L || position < duration * 0.05) return
        lifecycleScope.launch {
            App.instance.db.contentDao().upsertWatchHistory(
                WatchHistoryEntity(
                    streamId  = streamId,
                    title     = intent.getStringExtra(EXTRA_TITLE) ?: "",
                    posterUrl = intent.getStringExtra(EXTRA_POSTER),
                    streamUrl = intent.getStringExtra(EXTRA_URL) ?: return@launch,
                    ext       = intent.getStringExtra(EXTRA_EXT) ?: "mp4",
                    positionMs = position,
                    durationMs = duration,
                    isSeries  = intent.getBooleanExtra(EXTRA_IS_SERIES, false),
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun deleteWatchHistory() {
        val streamId = intent.getStringExtra(EXTRA_STREAM_ID) ?: return
        lifecycleScope.launch {
            App.instance.db.contentDao().deleteWatchHistory(streamId)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        saveWatchHistory()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK ||
            keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
