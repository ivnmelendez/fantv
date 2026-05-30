package com.primetv.app.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.TimeBar
import com.primetv.app.App
import com.primetv.app.R
import com.primetv.app.data.db.entity.WatchHistoryEntity
import com.primetv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var isLiveStream = false
    private val uiHandler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            uiHandler.postDelayed(this, 500)
        }
    }
    private val hideControlsRunnable = Runnable { hideControls() }

    companion object {
        const val EXTRA_URL          = "stream_url"
        const val EXTRA_TITLE        = "title"
        const val EXTRA_STREAM_ID    = "stream_id"
        const val EXTRA_POSTER       = "poster_url"
        const val EXTRA_EXT          = "stream_ext"
        const val EXTRA_POSITION     = "start_position_ms"
        const val EXTRA_IS_SERIES    = "is_series"
        const val EXTRA_SERIES_ID    = "series_id"
        const val EXTRA_SERIES_TITLE = "series_title"
        const val EXTRA_IS_LIVE      = "is_live"
        private const val CONTROLS_TIMEOUT_MS = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url            = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        isLiveStream       = intent.getBooleanExtra(EXTRA_IS_LIVE, false)
        val intentPosition = intent.getLongExtra(EXTRA_POSITION, 0L)
        val streamId       = intent.getStringExtra(EXTRA_STREAM_ID)

        if (isLiveStream || intentPosition > 0L || streamId == null) {
            initPlayer(url, intentPosition)
        } else {
            lifecycleScope.launch {
                val saved = App.instance.db.contentDao().getWatchHistoryEntry(streamId)?.positionMs ?: 0L
                initPlayer(url, saved)
            }
        }
    }

    private fun initPlayer(url: String, startPositionMs: Long = 0L) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            if (!isLiveStream) {
                setupControls(exo)
            }

            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isLiveStream) {
                        binding.playerControls.btnPlayPause.setImageResource(
                            if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                        )
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (!isLiveStream) updateTrackButtons(tracks)
                }

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

    private fun setupControls(exo: ExoPlayer) {
        val c = binding.playerControls

        c.playerTimebar.setKeyTimeIncrement(10_000)
        c.playerTimebar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                uiHandler.removeCallbacks(updateProgressRunnable)
            }
            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                c.playerPosition.text = formatTime(position)
            }
            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                if (!canceled) exo.seekTo(position)
                uiHandler.post(updateProgressRunnable)
                scheduleHideControls()
            }
        })

        c.btnPlayPause.setOnClickListener {
            exo.playWhenReady = !exo.playWhenReady
            scheduleHideControls()
        }
        c.btnSubtitle.setOnClickListener { showTrackPicker(C.TRACK_TYPE_TEXT) }
        c.btnAudio.setOnClickListener    { showTrackPicker(C.TRACK_TYPE_AUDIO) }

        uiHandler.post(updateProgressRunnable)
    }

    private fun updateProgress() {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0 } ?: 0L
        val position = exo.currentPosition
        val c = binding.playerControls
        c.playerTimebar.setDuration(duration)
        c.playerTimebar.setPosition(position)
        c.playerTimebar.setBufferedPosition(exo.bufferedPosition)
        c.playerPosition.text = formatTime(position)
        c.playerDuration.text = formatTime(duration)
    }

    private fun updateTrackButtons(tracks: Tracks) {
        val c = binding.playerControls
        c.btnSubtitle.visibility = if (tracks.groups.any { it.type == C.TRACK_TYPE_TEXT }) View.VISIBLE else View.GONE
        c.btnAudio.visibility    = if (tracks.groups.count { it.type == C.TRACK_TYPE_AUDIO } > 1) View.VISIBLE else View.GONE
    }

    private fun showControls() {
        binding.playerControls.root.visibility = View.VISIBLE
        binding.playerControls.btnPlayPause.requestFocus()
        scheduleHideControls()
    }

    private fun hideControls() {
        binding.playerControls.root.visibility = View.INVISIBLE
        uiHandler.removeCallbacks(hideControlsRunnable)
        binding.playerView.requestFocus()
    }

    private fun scheduleHideControls() {
        uiHandler.removeCallbacks(hideControlsRunnable)
        uiHandler.postDelayed(hideControlsRunnable, CONTROLS_TIMEOUT_MS)
    }

    private fun controlsVisible() = binding.playerControls.root.visibility == View.VISIBLE

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, sec)
        else String.format("%d:%02d", m, sec)
    }

    private fun showTrackPicker(trackType: Int) {
        val exo    = player ?: return
        val groups = exo.currentTracks.groups.filter { it.type == trackType }
        if (groups.isEmpty()) return
        val labels = groups.mapIndexed { i, group ->
            val fmt = group.getTrackFormat(0)
            when {
                !fmt.language.isNullOrEmpty() -> Locale(fmt.language!!).displayLanguage
                !fmt.label.isNullOrEmpty()    -> fmt.label!!
                else                          -> "Pista ${i + 1}"
            }
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(if (trackType == C.TRACK_TYPE_TEXT) "Subtítulos" else "Audio")
            .setItems(labels) { _, which ->
                exo.trackSelectionParameters = exo.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(TrackSelectionOverride(groups[which].mediaTrackGroup, 0))
                    .build()
            }
            .show()
    }

    private fun saveWatchHistory() {
        val streamId = intent.getStringExtra(EXTRA_STREAM_ID) ?: return
        val exo      = player ?: return
        val position = exo.currentPosition
        val duration = exo.duration.takeIf { it > 0 } ?: return
        if (position < 60_000L || position < duration * 0.05) return
        val seriesId     = intent.getStringExtra(EXTRA_SERIES_ID)
        val displayTitle = intent.getStringExtra(EXTRA_SERIES_TITLE)
            ?: intent.getStringExtra(EXTRA_TITLE) ?: ""
        lifecycleScope.launch {
            App.instance.db.contentDao().upsertWatchHistory(
                WatchHistoryEntity(
                    streamId   = streamId,
                    title      = displayTitle,
                    posterUrl  = intent.getStringExtra(EXTRA_POSTER),
                    streamUrl  = intent.getStringExtra(EXTRA_URL) ?: return@launch,
                    ext        = intent.getStringExtra(EXTRA_EXT) ?: "mp4",
                    positionMs = position,
                    durationMs = duration,
                    isSeries   = intent.getBooleanExtra(EXTRA_IS_SERIES, false),
                    watchedAt  = System.currentTimeMillis(),
                    seriesId   = seriesId
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
        if (!isLiveStream) uiHandler.post(updateProgressRunnable)
    }

    override fun onPause() {
        super.onPause()
        saveWatchHistory()
        player?.playWhenReady = false
        uiHandler.removeCallbacks(updateProgressRunnable)
        uiHandler.removeCallbacks(hideControlsRunnable)
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_BACK,
            android.view.KeyEvent.KEYCODE_ESCAPE -> {
                if (controlsVisible()) hideControls() else finish()
                return true
            }
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                if (!isLiveStream) {
                    if (controlsVisible()) scheduleHideControls() else showControls()
                    return true
                }
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!isLiveStream && !controlsVisible()) {
                    player?.let { it.seekTo(it.currentPosition + 10_000L) }
                    return true
                }
            }
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isLiveStream && !controlsVisible()) {
                    player?.let { it.seekTo((it.currentPosition - 10_000L).coerceAtLeast(0L)) }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
