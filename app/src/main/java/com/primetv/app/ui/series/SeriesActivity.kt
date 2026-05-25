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
import com.primetv.app.ui.detail.DetailActivity
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

        val seriesId    = intent.getIntExtra(MainActivity.EXTRA_STREAM_ID, -1)
        val title       = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: ""
        val backdropUrl = intent.getStringExtra(DetailActivity.EXTRA_BACKDROP_URL)
        if (seriesId == -1) { finish(); return }

        binding.tvTitle.text = title

        // Load backdrop immediately if passed from DetailActivity
        if (!backdropUrl.isNullOrBlank()) {
            Glide.with(this).load(backdropUrl).into(binding.backgroundImg)
        }

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

                si?.name?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }
                binding.tvDescription.text = si?.plot ?: ""
                binding.tvRating.text      = si?.rating ?: ""
                binding.tvYear.text        = si?.releaseDate?.take(4) ?: ""

                // Derive seasons from episodes map if the seasons list is absent
                val seasons = info.seasons?.takeIf { it.isNotEmpty() }
                    ?: allEpisodes.keys
                        .mapNotNull { it.toIntOrNull() }
                        .sorted()
                        .map { n ->
                            Season(
                                airDate = null,
                                episodeCount = allEpisodes[n.toString()]?.size,
                                id = null,
                                name = "Temporada $n",
                                overview = null,
                                seasonNumber = n,
                                cover = null,
                                coverBig = null
                            )
                        }

                val seasonCount = seasons.size
                binding.tvSeasons.text = if (seasonCount == 1) "1 Temporada" else "$seasonCount Temporadas"

                // Backdrop fallback to cover if not already loaded from intent
                val passedBackdrop = intent.getStringExtra(DetailActivity.EXTRA_BACKDROP_URL)
                if (passedBackdrop.isNullOrBlank()) {
                    val coverUrl = si?.backdropPath?.firstOrNull()?.takeIf { it.isNotBlank() } ?: si?.cover
                    Glide.with(this@SeriesActivity).load(coverUrl).into(binding.backgroundImg)
                }

                // Cast
                val cast = si?.cast?.takeIf { it.isNotBlank() }
                if (cast != null) {
                    binding.tvActors.text = cast
                    binding.linActors.visibility = View.VISIBLE
                }

                setupSeasons(seasons)

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
