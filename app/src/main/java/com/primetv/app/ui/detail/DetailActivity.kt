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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val streamId = intent.getIntExtra(MainActivity.EXTRA_STREAM_ID, -1)
        val title    = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: ""
        val isSeries = intent.getBooleanExtra(MainActivity.EXTRA_IS_SERIES, false)

        if (streamId == -1) { finish(); return }

        binding.tvTitle.text = title
        showLoading(true)

        if (isSeries) {
            loadSeriesInfo(streamId)
        } else {
            loadVodInfo(streamId)
        }
    }

    private fun loadVodInfo(vodId: Int) {
        lifecycleScope.launch {
            try {
                val info = repo.getVodInfo(vodId)
                val i = info.info
                val movie = info.movieData

                showLoading(false)
                i ?: return@launch

                binding.tvTitle.text = i.name ?: binding.tvTitle.text
                binding.tvDescription.text = i.plot ?: i.description ?: ""
                binding.tvActors.text = i.actors ?: i.cast ?: ""
                binding.tvGenre.text = i.genre ?: ""
                binding.tvYear.text = i.releasedate?.take(4) ?: ""
                binding.tvRating.text = i.rating?.let { "★ $it" } ?: ""
                binding.tvRated.text = i.mpaaRating ?: ""
                binding.tvDuration.text = i.duration ?: ""

                val posterUrl = i.coverBig ?: i.movieImage
                Glide.with(this@DetailActivity).load(posterUrl).into(binding.poster)
                Glide.with(this@DetailActivity)
                    .load(i.backdropPath?.firstOrNull() ?: posterUrl)
                    .into(binding.backgroundImg)

                val ext = movie?.containerExtension ?: "mp4"
                val url = repo.buildVodUrl(vodId, ext)
                setupPlayButton(url, i.name ?: "")

            } catch (_: Exception) {
                showLoading(false)
            }
        }
    }

    private fun loadSeriesInfo(seriesId: Int) {
        lifecycleScope.launch {
            try {
                val info = repo.getSeriesInfo(seriesId)
                showLoading(false)
                val si = info.info ?: return@launch

                binding.tvTitle.text = si.name ?: binding.tvTitle.text
                binding.tvDescription.text = si.plot ?: ""
                binding.tvActors.text = si.cast ?: ""
                binding.tvGenre.text = si.genre ?: ""
                binding.tvYear.text = si.releaseDate?.take(4) ?: ""
                binding.tvRating.text = si.rating?.let { "★ $it" } ?: ""

                Glide.with(this@DetailActivity).load(si.cover).into(binding.poster)
                Glide.with(this@DetailActivity)
                    .load(si.backdropPath ?: si.cover)
                    .into(binding.backgroundImg)

                // For series, "Play" goes to SeriesActivity to pick season/episode
                binding.gridActions.visibility = View.GONE
                setupSeriesButton(seriesId, si.name ?: "")

            } catch (_: Exception) {
                showLoading(false)
            }
        }
    }

    private fun setupPlayButton(streamUrl: String, title: String) {
        // TODO: set up action grid with Play, Trailer, Favorite buttons
        // For now, tapping background navigates directly to player
        binding.backgroundImg.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_URL, streamUrl)
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

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.root.visibility = if (show) View.VISIBLE else View.GONE
    }
}
