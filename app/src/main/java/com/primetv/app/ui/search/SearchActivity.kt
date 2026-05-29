package com.primetv.app.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.primetv.app.App
import com.primetv.app.data.db.entity.SeriesEntity
import com.primetv.app.data.db.entity.VodStreamEntity
import com.primetv.app.data.model.VodStream
import com.primetv.app.databinding.ActivitySearchBinding
import com.primetv.app.ui.detail.DetailActivity
import com.primetv.app.ui.main.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var query = StringBuilder()
    private var searchJob: Job? = null
    private lateinit var resultsAdapter: SearchResultsAdapter

    private val keys: List<String> = listOf("⎵", "⌫") +
            ('A'..'Z').map { it.toString() } +
            ('0'..'9').map { it.toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupKeyboard()
        setupResults()
        loadCategories()
    }

    private fun setupKeyboard() {
        val keyAdapter = SearchKeyAdapter(keys) { key ->
            when (key) {
                "⌫" -> if (query.isNotEmpty()) query.deleteCharAt(query.lastIndex)
                "⎵" -> query.append(' ')
                else -> query.append(key)
            }
            val display = query.toString().trim()
            binding.tvQuery.text = display.ifEmpty { null }
            scheduleSearch(display)
        }

        val glm = GridLayoutManager(this, 6)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (position) {
                0 -> 4  // space
                1 -> 2  // backspace
                else -> 1
            }
        }
        binding.rvKeyboard.layoutManager = glm
        binding.rvKeyboard.adapter = keyAdapter
        binding.rvKeyboard.itemAnimator = null
    }

    private fun setupResults() {
        resultsAdapter = SearchResultsAdapter(emptyList()) { item -> openDetail(item) }
        binding.rvResults.layoutManager = GridLayoutManager(this, 5)
        binding.rvResults.adapter = resultsAdapter
        binding.rvResults.itemAnimator = null
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val cats = App.instance.db.contentDao().getCategories("vod") +
                       App.instance.db.contentDao().getCategories("series")
            val names = cats.map { it.name }.filter { it.isNotBlank() }.distinct().sorted()
            val catAdapter = SearchCategoryAdapter(names) { name ->
                query.clear()
                query.append(name)
                binding.tvQuery.text = name
                scheduleSearch(name)
            }
            binding.rvCategories.layoutManager = LinearLayoutManager(this@SearchActivity)
            binding.rvCategories.adapter = catAdapter
        }
    }

    private fun scheduleSearch(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            binding.rvResults.visibility = View.GONE
            return
        }
        searchJob = lifecycleScope.launch {
            delay(200)
            val dao = App.instance.db.contentDao()
            val vod = dao.searchVodStreams(q).mapNotNull { runCatching { it.toModel() }.getOrNull() }
            val series = dao.searchSeries(q).map { it.toSearchVodStream() }
            val combined = (vod + series).sortedBy { it.name }
            resultsAdapter.update(combined)
            binding.rvResults.visibility = if (combined.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun SeriesEntity.toSearchVodStream() = VodStream(
        num = num, name = name, streamId = seriesId,
        streamIcon = cover, rating = rating, rating5Based = rating5Based,
        added = lastModified, categoryId = categoryId,
        containerExtension = "series", customSid = null, directSource = null
    )

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
