package com.primetv.app.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.primetv.app.App
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ActivityFavoritesBinding
import com.primetv.app.ui.detail.DetailActivity
import com.primetv.app.ui.main.MainActivity
import com.primetv.app.ui.search.SearchResultsAdapter
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val items = App.instance.db.contentDao().getFavorites().map { fav ->
                VodStream(
                    num = 0, name = fav.title, streamId = fav.streamId.toIntOrNull() ?: 0,
                    streamIcon = fav.posterUrl, rating = null, rating5Based = null,
                    added = null, categoryId = "favorites",
                    containerExtension = if (fav.isSeries) "series" else fav.ext,
                    customSid = null, directSource = null
                )
            }
            if (items.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvFavorites.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvFavorites.visibility = View.VISIBLE
                val adapter = SearchResultsAdapter(items) { item -> openDetail(item) }
                binding.rvFavorites.layoutManager = GridLayoutManager(this@FavoritesActivity, 6)
                binding.rvFavorites.adapter = adapter
                binding.rvFavorites.itemAnimator = null
            }
        }
    }

    private fun openDetail(item: VodStream) {
        val isSeries = item.containerExtension == "series"
        startActivity(Intent(this, DetailActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_STREAM_ID, item.streamId)
            putExtra(MainActivity.EXTRA_TITLE, item.name)
            putExtra(MainActivity.EXTRA_IS_SERIES, isSeries)
            putExtra(MainActivity.EXTRA_STREAM_ICON, item.streamIcon)
            if (!isSeries) putExtra(MainActivity.EXTRA_STREAM_EXT, item.containerExtension)
        })
    }
}
