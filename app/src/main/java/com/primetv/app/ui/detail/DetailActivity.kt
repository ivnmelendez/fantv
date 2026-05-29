package com.primetv.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.primetv.app.App
import com.primetv.app.R
import com.primetv.app.data.model.Episode
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityDetailBinding
import com.primetv.app.ui.main.MainActivity
import com.primetv.app.ui.player.PlayerActivity
import com.primetv.app.ui.series.SeriesActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BACKDROP_URL = "extra_backdrop_url"
    }

    private lateinit var binding: ActivityDetailBinding
    private val prefs get() = App.instance.prefs
    private val repo by lazy { XtreamRepository(prefs, App.instance.db) }
    private val tmdb get() = App.instance.tmdb

    private var backdropUrl: String? = null
    private var streamIcon: String? = null
    private var resumeEpisodeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val streamId = intent.getIntExtra(MainActivity.EXTRA_STREAM_ID, -1)
        val title    = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: ""
        val isSeries = intent.getBooleanExtra(MainActivity.EXTRA_IS_SERIES, false)
        val ext      = intent.getStringExtra(MainActivity.EXTRA_STREAM_EXT) ?: "mp4"
        streamIcon      = intent.getStringExtra(MainActivity.EXTRA_STREAM_ICON)
        resumeEpisodeId = intent.getStringExtra(MainActivity.EXTRA_RESUME_EPISODE_ID)

        if (streamId == -1) { finish(); return }

        binding.tvTitle.text = title

        if (isSeries) {
            binding.btnPlayPrimary.text = getString(R.string.detail_play_primary)
            binding.btnPlaySecondary.text = getString(R.string.detail_see_seasons)
            binding.btnPlaySecondary.visibility = View.VISIBLE
            binding.btnPlaySecondary.setOnClickListener { openSeries(streamId, title) }

            val epId = resumeEpisodeId
            if (epId != null) {
                lifecycleScope.launch {
                    val entry = App.instance.db.contentDao().getWatchHistoryEntry(epId)
                    if (entry != null) {
                        binding.btnPlayPrimary.text = getString(R.string.detail_continue)
                        binding.btnPlayPrimary.setOnClickListener {
                            playUrl(entry.streamUrl, title, epId, entry.ext, isSeries = true, seriesId = streamId.toString())
                        }
                    } else {
                        binding.btnPlayPrimary.setOnClickListener { openSeries(streamId, title) }
                    }
                }
            } else {
                binding.btnPlayPrimary.setOnClickListener { openSeries(streamId, title) }
            }

            // Load series info to get episode count and first episode for direct play
            lifecycleScope.launch {
                try {
                    val info = repo.getSeriesInfo(streamId)
                    val seasons = info.seasons ?: emptyList()
                    val allEpisodes = info.episodes ?: emptyMap()

                    // Season count in duration field — fall back to episode map keys if seasons absent
                    val seasonCount = seasons.size.takeIf { it > 0 }
                        ?: allEpisodes.keys.mapNotNull { it.toIntOrNull() }.distinct().size
                    binding.tvDuration.text = if (seasonCount == 1) "1 Temporada" else "$seasonCount Temporadas"

                    // Find first episode of season 1 (or lowest season)
                    val firstSeasonKey = allEpisodes.keys
                        .mapNotNull { it.toIntOrNull() }
                        .minOrNull()
                        ?.toString()
                    val firstEpisode: Episode? = firstSeasonKey
                        ?.let { allEpisodes[it] }
                        ?.minByOrNull { it.episodeNum }

                    if (firstEpisode != null && resumeEpisodeId == null) {
                        val sNum = firstEpisode.season.toString().padStart(2, '0')
                        val eNum = firstEpisode.episodeNum.toString().padStart(2, '0')
                        binding.btnPlayPrimary.text = "▶  Reproducir T$sNum:E$eNum"
                        binding.btnPlayPrimary.setOnClickListener {
                            val id  = firstEpisode.id ?: return@setOnClickListener
                            val epExt = firstEpisode.containerExtension ?: "mp4"
                            val url = repo.buildSeriesUrl(id, epExt)
                            playUrl(url, firstEpisode.title ?: "T$sNum:E$eNum", id.toString(), epExt, isSeries = true, seriesId = streamId.toString())
                        }
                    }
                } catch (_: Exception) {
                    // Keep fallback button behaviour (opens series browser)
                }
            }
        } else {
            val vodUrl = repo.buildVodUrl(streamId, ext)
            binding.btnPlayPrimary.setOnClickListener { playUrl(vodUrl, title, streamId.toString(), ext, isSeries = false) }
        }

        val favId = streamId.toString()
        lifecycleScope.launch {
            val inFav = App.instance.db.contentDao().getFavorite(favId) != null
            binding.btnFavorites.text = getString(if (inFav) R.string.detail_remove_favorite else R.string.detail_add_favorite)
        }
        binding.btnFavorites.setOnClickListener {
            lifecycleScope.launch {
                val dao = App.instance.db.contentDao()
                if (dao.getFavorite(favId) != null) {
                    dao.removeFavorite(favId)
                    binding.btnFavorites.text = getString(R.string.detail_add_favorite)
                } else {
                    dao.addFavorite(
                        com.primetv.app.data.db.entity.FavoriteEntity(
                            streamId = favId,
                            title = title,
                            posterUrl = streamIcon,
                            isSeries = isSeries,
                            ext = if (isSeries) "series" else ext
                        )
                    )
                    binding.btnFavorites.text = getString(R.string.detail_remove_favorite)
                }
            }
        }

        val sidStr = streamId.toString()
        val episodeId = resumeEpisodeId
        lifecycleScope.launch {
            val inHistory = if (isSeries) {
                App.instance.db.contentDao().getWatchHistoryEntryBySeriesId(sidStr) != null
                    || (episodeId != null && App.instance.db.contentDao().getWatchHistoryEntry(episodeId) != null)
            } else {
                App.instance.db.contentDao().getWatchHistoryEntry(sidStr) != null
            }
            if (inHistory) binding.btnRemoveHistory.visibility = View.VISIBLE
        }
        binding.btnRemoveHistory.setOnClickListener {
            lifecycleScope.launch {
                if (isSeries) {
                    App.instance.db.contentDao().deleteWatchHistoryBySeriesId(sidStr)
                    episodeId?.let { App.instance.db.contentDao().deleteWatchHistory(it) }
                } else {
                    App.instance.db.contentDao().deleteWatchHistory(sidStr)
                }
                binding.btnRemoveHistory.visibility = View.GONE
            }
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

            backdropUrl = tmdb.backdropUrl(result.backdropPath)
            Glide.with(this@DetailActivity).load(backdropUrl).into(binding.backgroundImg)

            result.id?.let { tmdbId ->
                if (isSeries) {
                    // No runtime for series — duration field used for season count; only fetch cast
                    val cast = async { tmdb.getCast(tmdbId, isSeries) }.await()
                    cast?.let {
                        binding.tvActors.text = it
                        binding.linActors.visibility = View.VISIBLE
                    }
                } else {
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
    }

    private fun playUrl(url: String, title: String, streamId: String = "", ext: String = "mp4", isSeries: Boolean = false, seriesId: String? = null) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
            if (streamId.isNotEmpty()) putExtra(PlayerActivity.EXTRA_STREAM_ID, streamId)
            putExtra(PlayerActivity.EXTRA_POSTER, streamIcon)
            putExtra(PlayerActivity.EXTRA_EXT, ext)
            putExtra(PlayerActivity.EXTRA_IS_SERIES, isSeries)
            seriesId?.let { putExtra(PlayerActivity.EXTRA_SERIES_ID, it) }
            if (isSeries) putExtra(PlayerActivity.EXTRA_SERIES_TITLE, title)
        })
    }

    private fun openSeries(seriesId: Int, title: String) {
        startActivity(Intent(this, SeriesActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_STREAM_ID, seriesId)
            putExtra(MainActivity.EXTRA_TITLE, title)
            putExtra(EXTRA_BACKDROP_URL, backdropUrl)
            putExtra(MainActivity.EXTRA_STREAM_ICON, streamIcon)
            resumeEpisodeId?.let { putExtra(MainActivity.EXTRA_RESUME_EPISODE_ID, it) }
        })
    }
}
