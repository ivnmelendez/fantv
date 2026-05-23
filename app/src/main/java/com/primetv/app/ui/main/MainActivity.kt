package com.primetv.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.primetv.app.App
import com.primetv.app.R
import com.primetv.app.data.model.MenuItem
import com.primetv.app.data.model.VodStream
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityMainBinding
import com.primetv.app.ui.detail.DetailActivity
import com.primetv.app.ui.live.LiveActivity
import com.primetv.app.ui.player.PlayerActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var repo: XtreamRepository
    private val prefs get() = App.instance.prefs

    private var featuredItem: VodStream? = null
    private var rowsAdapter: RowsAdapter? = null

    // Menu IDs
    companion object {
        const val MENU_HOME     = 0
        const val MENU_MOVIES   = 1
        const val MENU_SERIES   = 2
        const val MENU_LIVE     = 3
        const val MENU_SEARCH   = 4
        const val MENU_FAVS     = 5
        const val MENU_SETTINGS = 6

        const val EXTRA_STREAM_ID  = "stream_id"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_STREAM_EXT = "stream_ext"
        const val EXTRA_TITLE      = "title"
        const val EXTRA_IS_SERIES  = "is_series"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = XtreamRepository(prefs, App.instance.db)
        viewModel = ViewModelProvider(this, MainViewModelFactory(repo))[MainViewModel::class.java]

        setupMenu()
        observeState()
        loadMenu(prefs.activeMenuIndex)
    }

    private fun setupMenu() {
        val menuItems = listOf(
            MenuItem(MENU_HOME,     getString(R.string.nav_home),     R.drawable.ic_nav_home),
            MenuItem(MENU_MOVIES,   getString(R.string.nav_movies),   R.drawable.ic_nav_movie),
            MenuItem(MENU_SERIES,   getString(R.string.nav_series),   R.drawable.ic_nav_series),
            MenuItem(MENU_LIVE,     getString(R.string.nav_live),     R.drawable.ic_nav_live),
            MenuItem(MENU_SEARCH,   getString(R.string.nav_search),   R.drawable.ic_nav_search),
            MenuItem(MENU_FAVS,     getString(R.string.nav_favorites),R.drawable.ic_nav_favorite),
            MenuItem(MENU_SETTINGS, getString(R.string.nav_settings), R.drawable.ic_nav_settings),
        )

        val menuAdapter = MenuAdapter(menuItems) { menuId ->
            prefs.activeMenuIndex = menuId
            loadMenu(menuId)
        }

        binding.menuLayout.menuGrid.adapter = menuAdapter
        menuAdapter.selectIndex(prefs.activeMenuIndex.coerceIn(0, menuItems.lastIndex))
    }

    private fun loadMenu(menuId: Int) {
        when (menuId) {
            MENU_HOME -> viewModel.loadHome()
            MENU_MOVIES -> viewModel.loadMovies()
            MENU_SERIES -> viewModel.loadSeries()
            MENU_LIVE -> startActivity(Intent(this, LiveActivity::class.java))
            else -> { /* TODO: search, favorites, settings */ }
        }
    }

    private fun observeState() {
        viewModel.sportsRefreshed.observe(this) { sports ->
            rowsAdapter?.updateRow("home_sports", sports)
        }
        viewModel.state.observe(this) { state ->
            when (state) {
                is MainState.Loading -> showLoading(state.message)
                is MainState.Success -> {
                    hideLoading()
                    featuredItem = state.featuredItem
                    bindFeatured(state.featuredItem)
                    binding.rvRows.apply {
                        itemAnimator = null
                        layoutManager = LinearLayoutManager(this@MainActivity)
                        rowsAdapter = RowsAdapter(
                            rows = state.rows,
                            onItemClick = { item -> onItemClick(item) },
                            onItemFocus = { item ->
                                bindFeatured(item)
                                viewModel.loadFeaturedInfo(item.streamId, item.name, item.containerExtension == "series")
                            },
                            onRefreshRow = { viewModel.refreshSportsRow() }
                        )
                        adapter = rowsAdapter
                    }
                    state.featuredItem?.let { item ->
                        viewModel.loadFeaturedInfo(item.streamId, item.name, item.containerExtension == "series")
                    }
                }
                is MainState.Error -> {
                    hideLoading()
                    android.widget.Toast.makeText(this@MainActivity, state.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.featuredInfo.observe(this) { info ->
            info ?: return@observe
            binding.tvDescription.text = info.plot ?: ""
            binding.tvDescription.visibility = if (info.plot.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvGenre.text = info.genre ?: ""
            binding.tvGenre.visibility = if (info.genre.isNullOrBlank()) View.GONE else View.VISIBLE
            if (!info.rating.isNullOrBlank()) binding.tvRating.text = info.rating
            if (!info.releaseDate.isNullOrBlank()) {
                val year = info.releaseDate.take(4)
                if (year.length == 4 && year.all { it.isDigit() }) binding.tvYear.text = year
            }
            if (!info.backdropPath.isNullOrBlank()) {
                Glide.with(this).load(info.backdropPath)
                    .transition(DrawableTransitionOptions.withCrossFade(400))
                    .into(binding.ivBackground)
            }
        }
    }

    private fun bindFeatured(item: VodStream?) {
        item ?: return
        binding.cardInfo.visibility = View.VISIBLE
        Glide.with(this).clear(binding.ivBackground)
        binding.ivBackground.setImageDrawable(null)
        binding.tvFeatTitle.text = item.name
        val isLive = item.containerExtension == "live"
        binding.tvImdbBadge.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.tvRating.visibility = if (isLive) View.GONE else View.VISIBLE
        if (!isLive) binding.tvRating.text = item.rating ?: "N/A"
        binding.tvDescription.visibility = View.GONE
        binding.tvGenre.visibility = View.GONE
        binding.tvSeasons.visibility = View.GONE

        val year = item.added?.toLongOrNull()?.let { ts ->
            Calendar.getInstance().apply { timeInMillis = ts * 1000L }.get(Calendar.YEAR).toString()
        }
        binding.tvYear.text = year ?: ""

        binding.btnPlayFeat.setOnClickListener { onItemClick(item) }
    }

    private fun onItemClick(item: VodStream) {
        if (item.containerExtension == "series") {
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(EXTRA_STREAM_ID, item.streamId)
                putExtra(EXTRA_TITLE, item.name)
                putExtra(EXTRA_IS_SERIES, true)
            }
            startActivity(intent)
        } else if (item.containerExtension == "live") {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_URL, repo.buildLiveUrl(item.streamId))
                putExtra(PlayerActivity.EXTRA_TITLE, item.name)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(EXTRA_STREAM_ID, item.streamId)
                putExtra(EXTRA_TITLE, item.name)
                putExtra(EXTRA_IS_SERIES, false)
                putExtra(EXTRA_STREAM_EXT, item.containerExtension ?: "mp4")
            }
            startActivity(intent)
        }
    }

    private fun showLoading(message: String = "") {
        binding.loadingLayout.root.visibility = View.VISIBLE
        binding.loadingLayout.tvLoadingMessage.apply {
            visibility = if (message.isNotEmpty()) View.VISIBLE else View.GONE
            text = message
        }
    }

    private fun hideLoading() {
        binding.loadingLayout.root.visibility = View.GONE
    }
}
