package com.primetv.app.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.primetv.app.App
import com.primetv.app.data.model.Episode
import com.primetv.app.data.model.Season
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivitySeriesBinding
import com.primetv.app.ui.main.MainActivity
import com.primetv.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class SeriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesBinding
    private val prefs get() = App.instance.prefs
    private val repo by lazy { XtreamRepository(prefs, App.instance.db) }

    private var allEpisodes: Map<String, List<Episode>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val seriesId = intent.getIntExtra(MainActivity.EXTRA_STREAM_ID, -1)
        if (seriesId == -1) { finish(); return }

        showLoading(true)
        loadSeries(seriesId)
    }

    private fun loadSeries(seriesId: Int) {
        lifecycleScope.launch {
            try {
                val info = repo.getSeriesInfo(seriesId)
                showLoading(false)

                val si = info.info
                allEpisodes = info.episodes ?: emptyMap()

                binding.tvTitle.text = si?.name ?: ""
                binding.tvDescription.text = si?.plot ?: ""
                binding.tvRating.text = si?.rating?.let { "★ $it" } ?: ""
                binding.tvYear.text = si?.releaseDate?.take(4) ?: ""

                Glide.with(this@SeriesActivity).load(si?.cover).into(binding.backgroundImg)

                setupSeasons(info.seasons ?: emptyList())

            } catch (_: Exception) {
                showLoading(false)
            }
        }
    }

    private fun setupSeasons(seasons: List<Season>) {
        val seasonAdapter = SeasonAdapter(seasons) { season ->
            loadEpisodesForSeason(season.seasonNumber)
        }
        binding.rvSeasons.layoutManager = LinearLayoutManager(this)
        binding.rvSeasons.adapter = seasonAdapter

        if (seasons.isNotEmpty()) {
            loadEpisodesForSeason(seasons.first().seasonNumber)
        }
    }

    private fun loadEpisodesForSeason(seasonNumber: Int) {
        val episodes = allEpisodes[seasonNumber.toString()] ?: return
        val episodeAdapter = EpisodeAdapter(episodes) { episode ->
            playEpisode(episode)
        }
        binding.rvEpisodes.layoutManager = LinearLayoutManager(this)
        binding.rvEpisodes.adapter = episodeAdapter
    }

    private fun playEpisode(episode: Episode) {
        val id  = episode.id ?: return
        val ext = episode.containerExtension ?: "mp4"
        val url = repo.buildSeriesUrl(id, ext)
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_TITLE, episode.title ?: "Episode ${episode.episodeNum}")
        })
    }

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.root.visibility = if (show) View.VISIBLE else View.GONE
    }
}
