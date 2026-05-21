package com.primetv.app.ui.player

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.primetv.app.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    companion object {
        const val EXTRA_URL   = "stream_url"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url   = intent.getStringExtra(EXTRA_URL)   ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        initPlayer(url)
    }

    private fun initPlayer(url: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.pbBuffer.visibility = when (state) {
                        Player.STATE_BUFFERING -> View.VISIBLE
                        else -> View.GONE
                    }
                    if (state == Player.STATE_ENDED) finish()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    binding.pbBuffer.visibility = View.GONE
                    binding.rlayError.visibility = View.VISIBLE
                    binding.tvError.text = "Playback error: ${error.message}"
                }
            })

            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
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
