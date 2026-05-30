package com.primetv.app.ui.player

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.primetv.app.App
import com.primetv.app.R
import com.primetv.app.data.db.entity.WatchHistoryEntity
import com.primetv.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var btnSubtitle: ImageButton? = null
    private var btnAudio: ImageButton? = null

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url            = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val isLive         = intent.getBooleanExtra(EXTRA_IS_LIVE, false)
        val intentPosition = intent.getLongExtra(EXTRA_POSITION, 0L)
        val streamId       = intent.getStringExtra(EXTRA_STREAM_ID)

        if (isLive || intentPosition > 0L || streamId == null) {
            initPlayer(url, intentPosition, isLive)
        } else {
            lifecycleScope.launch {
                val saved = App.instance.db.contentDao().getWatchHistoryEntry(streamId)?.positionMs ?: 0L
                initPlayer(url, saved, false)
            }
        }
    }

    private fun initPlayer(url: String, startPositionMs: Long = 0L, isLive: Boolean = false) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            if (isLive) {
                binding.playerView.useController = false
            } else {
                btnSubtitle = binding.playerView.findViewById(R.id.btn_subtitle)
                btnAudio    = binding.playerView.findViewById(R.id.btn_audio)
                btnSubtitle?.setOnClickListener { showTrackPicker(C.TRACK_TYPE_TEXT) }
                btnAudio?.setOnClickListener    { showTrackPicker(C.TRACK_TYPE_AUDIO) }
            }

            exo.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    updateTrackButtons(tracks)
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

    private fun updateTrackButtons(tracks: Tracks) {
        val textGroups  = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        btnSubtitle?.visibility = if (textGroups.isNotEmpty()) View.VISIBLE else View.GONE
        btnAudio?.visibility    = if (audioGroups.size > 1)   View.VISIBLE else View.GONE
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

        val title = if (trackType == C.TRACK_TYPE_TEXT) "Subtítulos" else "Audio"
        AlertDialog.Builder(this)
            .setTitle(title)
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
        if (!binding.playerView.isControllerFullyVisible) {
            when (keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    player?.let { it.seekTo(it.currentPosition + 10_000L) }
                    return true
                }
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                    player?.let { it.seekTo((it.currentPosition - 10_000L).coerceAtLeast(0L)) }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
