package com.primetv.app.ui.live

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.primetv.app.App
import com.primetv.app.data.model.LiveStream
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityLiveBinding
import com.primetv.app.ui.player.PlayerActivity
import java.text.SimpleDateFormat
import java.util.*

class LiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveBinding
    private lateinit var viewModel: LiveViewModel
    private val prefs get() = App.instance.prefs

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

        observeState()
        viewModel.loadCategories()
        timeHandler.post(clockRunnable)
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LiveState.Loading -> showLoading(true)
                is LiveState.CategoriesLoaded -> {
                    showLoading(false)
                    setupCategoryGrid(state.categories.mapNotNull { cat ->
                        val name = cat.name ?: return@mapNotNull null
                        val id = cat.id ?: return@mapNotNull null
                        name to id
                    })
                }
                is LiveState.ChannelsLoaded -> {
                    showLoading(false)
                    setupChannelGrid(state.channels)
                    binding.channelsLayout.visibility = View.VISIBLE
                }
                is LiveState.Error -> showLoading(false)
            }
        }
    }

    private fun setupCategoryGrid(categories: List<Pair<String, String>>) {
        // Simple text-based category list
        val catAdapter = CategoryAdapter(categories) { catId ->
            viewModel.loadChannels(catId)
        }
        binding.categoryGrid.layoutManager = LinearLayoutManager(this)
        binding.categoryGrid.adapter = catAdapter
    }

    private fun setupChannelGrid(channels: List<LiveStream>) {
        val channelAdapter = ChannelAdapter(channels) { stream ->
            playChannel(stream)
        }
        binding.channelGrid.layoutManager = LinearLayoutManager(this)
        binding.channelGrid.adapter = channelAdapter
    }

    private fun playChannel(stream: LiveStream) {
        val url = prefs.let {
            "${it.normalizedServer()}/live/${it.username}/${it.password}/${stream.streamId}.m3u8"
        }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_TITLE, stream.name)
        })
    }

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        timeHandler.removeCallbacks(clockRunnable)
        super.onDestroy()
    }
}
