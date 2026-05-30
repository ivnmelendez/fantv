package com.primetv.app.ui.live

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.primetv.app.App
import com.primetv.app.data.model.LiveStream
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityLiveBinding
import java.text.SimpleDateFormat
import java.util.*

class LiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveBinding
    private lateinit var viewModel: LiveViewModel
    private val prefs get() = App.instance.prefs

    private var player: ExoPlayer? = null
    private var currentStream: LiveStream? = null
    private var currentChannelIndex = 0

    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hideOverlay() }

    private val timeHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            binding.tvDate.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            timeHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = XtreamRepository(prefs, App.instance.db)
        viewModel = ViewModelProvider(this, LiveViewModelFactory(repo))[LiveViewModel::class.java]

        initPlayer()
        observeState()
        viewModel.loadAll()
        timeHandler.post(clockRunnable)
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            binding.playerView.useController = false
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> binding.loadingLayout.root.visibility = View.VISIBLE
                        Player.STATE_READY -> {
                            binding.loadingLayout.root.visibility = View.GONE
                            binding.rlayOffline.visibility = View.GONE
                        }
                        else -> {}
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    binding.loadingLayout.root.visibility = View.GONE
                    binding.rlayOffline.visibility = View.VISIBLE
                }
            })
        }
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            when (state) {
                LiveState.Loading -> {
                    binding.loadingLayout.root.visibility = View.VISIBLE
                    showOverlay()
                }
                LiveState.Ready -> binding.loadingLayout.root.visibility = View.GONE
                is LiveState.Error -> binding.loadingLayout.root.visibility = View.GONE
            }
        }

        viewModel.categories.observe(this) { cats ->
            binding.categoryGrid.layoutManager = LinearLayoutManager(this)
            binding.categoryGrid.adapter = CategoryAdapter(cats) { catId ->
                viewModel.selectCategory(catId)
                resetAutoHide()
            }
        }

        viewModel.channels.observe(this) { channels ->
            binding.channelGrid.layoutManager = LinearLayoutManager(this)
            val adapter = ChannelAdapter(channels) { stream ->
                playChannel(stream)
                hideOverlay()
            }
            binding.channelGrid.adapter = adapter
            val current = currentStream
            if (current != null) {
                adapter.playingIndex = channels.indexOf(current).coerceAtLeast(0)
            } else if (channels.isNotEmpty()) {
                playChannel(channels.first())
                showOverlay()
            }
        }
    }

    private fun playChannel(stream: LiveStream) {
        val channels = viewModel.channels.value ?: emptyList()
        currentStream = stream
        currentChannelIndex = channels.indexOf(stream).coerceAtLeast(0)
        (binding.channelGrid.adapter as? ChannelAdapter)?.playingIndex = currentChannelIndex

        binding.tvCurrentChannel.text = stream.name
        Glide.with(this).load(stream.streamIcon).into(binding.ivCurrentLogo)

        val url = "${prefs.normalizedServer()}/live/${prefs.username}/${prefs.password}/${stream.streamId}.m3u8"
        player?.run {
            stop()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
        binding.rlayOffline.visibility = View.GONE
        binding.loadingLayout.root.visibility = View.VISIBLE
    }

    private fun zapChannel(delta: Int) {
        val channels = viewModel.channels.value ?: return
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + delta + channels.size) % channels.size
        playChannel(channels[currentChannelIndex])
    }

    private fun showOverlay() {
        binding.overlayPanel.visibility = View.VISIBLE
        binding.channelGrid.requestFocus()
        resetAutoHide()
    }

    private fun hideOverlay() {
        binding.overlayPanel.visibility = View.GONE
        autoHideHandler.removeCallbacks(autoHideRunnable)
    }

    private fun resetAutoHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
        autoHideHandler.postDelayed(autoHideRunnable, 5_000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        val overlayVisible = binding.overlayPanel.visibility == View.VISIBLE
        return if (!overlayVisible) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP   -> { zapChannel(-1); true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { zapChannel(+1); true }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    finish(); true
                }
                else -> { showOverlay(); true }
            }
        } else {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    hideOverlay(); true
                }
                else -> {
                    resetAutoHide()
                    super.dispatchKeyEvent(event)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        autoHideHandler.removeCallbacks(autoHideRunnable)
        timeHandler.removeCallbacks(clockRunnable)
        super.onDestroy()
    }
}
