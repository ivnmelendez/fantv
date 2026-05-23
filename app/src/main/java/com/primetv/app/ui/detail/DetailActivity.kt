package com.primetv.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.primetv.app.App
import com.primetv.app.R
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityDetailBinding
import com.primetv.app.ui.main.MainActivity
import com.primetv.app.ui.player.PlayerActivity
import com.primetv.app.ui.series.SeriesActivity
import kotlinx.coroutines.async
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
        val ext      = intent.getStringExtra(MainActivity.EXTRA_STREAM_EXT) ?: "mp4"

        if (streamId == -1) { finish(); return }

        binding.tvTitle.text = title

        if (isSeries) {
            binding.btnPlayPrimary.text = getString(R.string.detail_see_episodes)
            binding.btnPlayPrimary.setOnClickListener { openSeries(streamId, title) }
        } else {
            val vodUrl = repo.buildVodUrl(streamId, ext)
            binding.btnPlayPrimary.setOnClickListener { playUrl(vodUrl, title) }
        }

        binding.btnFavorites.setOnClickListener {
            Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val result = tmdb.search(title, isSeries) ?: return@launch

            binding.tvTitle.text = result.title ?: result.name ?: title
            binding.tvDescription.text = result.overview ?: ""
            binding.tvRating.text = result.voteAverage?.let { String.format("%.1f", it) } ?: ""
            binding.tvYear.text = (result.releaseDate ?: result.firstAirDate)?.take(4) ?: ""

            val genre = tmdb.genreNames(result.genreIds, isSeries)
            if (genre.isNotEmpty()) {
                binding.tvGenre.text = genre
                binding.linGenre.visibility = View.VISIBLE
            }

            val backdropUrl = tmdb.backdropUrl(result.backdropPath)
            Glide.with(this@DetailActivity).load(backdropUrl).into(binding.backgroundImg)

            result.id?.let { tmdbId ->
                val runtimeDeferred = async { tmdb.getRuntime(tmdbId, isSeries) }
                val castDeferred    = async { tmdb.getCast(tmdbId, isSeries) }

                runtimeDeferred.await()?.let { binding.tvDuration.text = it }

                castDeferred.await()?.let { cast ->
                    binding.tvActors.text = cast
                    binding.linActors.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun playUrl(url: String, title: String) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
        })
    }

    private fun openSeries(seriesId: Int, title: String) {
        startActivity(Intent(this, SeriesActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_STREAM_ID, seriesId)
            putExtra(MainActivity.EXTRA_TITLE, title)
        })
    }
}
