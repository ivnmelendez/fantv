package com.primetv.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.primetv.app.App
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityDetailBinding
import com.primetv.app.ui.main.MainActivity
import com.primetv.app.ui.player.PlayerActivity
import com.primetv.app.ui.series.SeriesActivity
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val prefs get() = App.instance.prefs
    private val repo by lazy { XtreamRepository(prefs, App.instance.db) }
    private val tmdb get() = App.instance.tmdb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val streamId = intent.getIntExtra(MainActivity.EXTRA_STREAM_ID, -1)
        val title    = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: ""
        val isSeries = intent.getBooleanExtra(MainActivity.EXTRA_IS_SERIES, false)

        if (streamId == -1) { finish(); return }

        binding.tvTitle.text = title

        // Setup play button immediately — no provider call needed
        if (isSeries) {
            setupSeriesButton(streamId, title)
        } else {
            setupPlayButton(repo.buildVodUrl(streamId, "mp4"), title)
        }

        // Load all metadata from TMDB — fast, no provider hit
        lifecycleScope.launch {
            val result = tmdb.search(title, isSeries) ?: return@launch

            binding.tvTitle.text = result.title ?: result.name ?: title
            binding.tvDescription.text = result.overview ?: ""
            binding.tvRating.text = result.voteAverage
                ?.let { "★ ${String.format("%.1f", it)}" } ?: ""
            binding.tvYear.text = (result.releaseDate ?: result.firstAirDate)?.take(4) ?: ""
            binding.tvGenre.text = tmdb.genreNames(result.genreIds, isSeries)

            val backdropUrl = tmdb.backdropUrl(result.backdropPath)
            val posterUrl   = result.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

            Glide.with(this@DetailActivity).load(backdropUrl).into(binding.backgroundImg)
            Glide.with(this@DetailActivity).load(posterUrl).into(binding.poster)
        }
    }

    private fun setupPlayButton(url: String, title: String) {
        binding.backgroundImg.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_URL, url)
                putExtra(PlayerActivity.EXTRA_TITLE, title)
            })
        }
    }

    private fun setupSeriesButton(seriesId: Int, title: String) {
        binding.backgroundImg.setOnClickListener {
            startActivity(Intent(this, SeriesActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_STREAM_ID, seriesId)
                putExtra(MainActivity.EXTRA_TITLE, title)
            })
        }
    }
}
