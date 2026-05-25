package com.primetv.app.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
    private var seasonAdapter: SeasonAdapter? = null

    private val focusGuard = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
        if (newFocus != null && isDescendantOf(newFocus, binding.rvEpisodes)) {
            binding.rvSeasons.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
    }

    private fun isDescendantOf(child: View, parent: View): Boolean {
        var v: View? = child
        while (v != null) {
            if (v == parent) return true
            v = v.parent as? View
        }
        return false
    }

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
        binding.root.viewTreeObserver.addOnGlobalFocusChangeListener(focusGuard)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.root.viewTreeObserver.removeOnGlobalFocusChangeListener(focusGuard)
    }

    private fun loadSeries(seriesId: Int) {
        lifecycleScope.launch {
            try {
                val info = repo.getSeriesInfo(seriesId)
                showLoading(false)

                val si = info.info
                allEpisodes = (info.episodes ?: emptyMap()).filterKeys { it != "0" }

                si?.name?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }
                binding.tvDescription.text = si?.plot ?: ""
                binding.tvRating.text      = si?.rating ?: ""
                binding.tvYear.text        = si?.releaseDate?.take(4) ?: ""

                // Derive seasons from episodes map if the seasons list is absent; skip season 0 (Especiales)
                val seasons = info.seasons?.filter { it.seasonNumber != 0 }?.takeIf { it.isNotEmpty() }
                    ?: allEpisodes.keys
                        .mapNotNull { it.toIntOrNull() }
                        .filter { it != 0 }
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
        seasonAdapter = SeasonAdapter(seasons) { season ->
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
        if (binding.rvEpisodes.layoutManager == null) {
            binding.rvEpisodes.layoutManager = LinearLayoutManager(this)
        }
        binding.rvEpisodes.post {
            binding.rvEpisodes.adapter = episodeAdapter
            binding.rvEpisodes.scrollToPosition(0)
        }
    }

    private fun playEpisode(episode: Episode) {
        val id  = episode.id ?: return
        val ext = episode.containerExtension ?: "mp4"
        val url = repo.buildSeriesUrl(id, ext)
        val posterUrl = intent.getStringExtra(MainActivity.EXTRA_STREAM_ICON)
            ?: intent.getStringExtra(DetailActivity.EXTRA_BACKDROP_URL)
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_TITLE, episode.title ?: "Episode ${episode.episodeNum}")
            putExtra(PlayerActivity.EXTRA_STREAM_ID, id.toString())
            putExtra(PlayerActivity.EXTRA_POSTER, posterUrl)
            putExtra(PlayerActivity.EXTRA_EXT, ext)
            putExtra(PlayerActivity.EXTRA_IS_SERIES, true)
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
            binding.rvEpisodes.hasFocus()) {
            binding.rvSeasons.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            val pos = seasonAdapter?.getSelectedPosition() ?: 0
            (binding.rvSeasons.layoutManager as? LinearLayoutManager)?.scrollToPosition(pos)
            binding.rvSeasons.post {
                binding.rvSeasons.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.root.visibility = if (show) View.VISIBLE else View.GONE
    }
}
