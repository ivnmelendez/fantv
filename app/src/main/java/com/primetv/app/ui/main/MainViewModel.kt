package com.primetv.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primetv.app.data.model.VodStream
import com.primetv.app.data.repository.XtreamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FeaturedInfo(
    val plot: String?,
    val genre: String?,
    val cast: String?,
    val releaseDate: String?,
    val rating: String?,
    val seasonCount: Int?,
    val backdropPath: String?
)

data class ContentRow(
    val categoryId: String,
    val categoryName: String,
    val items: List<VodStream>
)

sealed class MainState {
    object Loading : MainState()
    data class Success(
        val featuredItem: VodStream?,
        val rows: List<ContentRow>
    ) : MainState()
    data class Error(val message: String) : MainState()
}

class MainViewModel(private val repo: XtreamRepository) : ViewModel() {

    private val _state = MutableLiveData<MainState>(MainState.Loading)
    val state: LiveData<MainState> = _state

    private val _featuredInfo = MutableLiveData<FeaturedInfo?>()
    val featuredInfo: LiveData<FeaturedInfo?> = _featuredInfo

    private var featuredInfoJob: Job? = null
    private val infoCache = HashMap<Int, FeaturedInfo>()

    fun loadFeaturedInfo(streamId: Int, isSeries: Boolean) {
        featuredInfoJob?.cancel()
        // Cache hit: synchronous, no coroutine needed
        infoCache[streamId]?.let { cached ->
            _featuredInfo.value = cached
            return
        }
        featuredInfoJob = viewModelScope.launch {
            delay(350)
            val info = if (isSeries) {
                try {
                    repo.getSeriesInfo(streamId).let { resp ->
                        val seasons = resp.seasons?.size
                        FeaturedInfo(
                            plot = resp.info?.plot,
                            genre = resp.info?.genre,
                            cast = resp.info?.cast,
                            releaseDate = resp.info?.releaseDate,
                            rating = resp.info?.rating,
                            seasonCount = seasons,
                            backdropPath = resp.info?.backdropPath?.takeIf { it.isNotBlank() }
                                ?: resp.info?.cover?.takeIf { it.isNotBlank() }
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) { null }
            } else {
                try {
                    repo.getVodInfo(streamId).let { resp ->
                        FeaturedInfo(
                            plot = resp.info?.plot ?: resp.info?.description,
                            genre = resp.info?.genre,
                            cast = resp.info?.cast ?: resp.info?.actors,
                            releaseDate = resp.info?.releasedate,
                            rating = resp.info?.rating?.toString(),
                            seasonCount = null,
                            backdropPath = resp.info?.backdropPath?.firstOrNull()?.takeIf { it.isNotBlank() }
                                ?: resp.info?.coverBig?.takeIf { it.isNotBlank() }
                                ?: resp.info?.movieImage?.takeIf { it.isNotBlank() }
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) { null }
            }
            info?.let { infoCache[streamId] = it }
            _featuredInfo.value = info
        }
    }

    fun loadHome() {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                val movies = runCatching { repo.getVodStreams() }.getOrElse { emptyList() }
                    .sortedByDescending { it.added.orEmpty() }

                val series = runCatching { repo.getSeries() }.getOrElse { emptyList() }
                    .map { s ->
                        VodStream(
                            num = s.num,
                            name = s.name,
                            streamId = s.seriesId,
                            streamIcon = s.cover,
                            rating = s.rating,
                            rating5Based = s.rating5Based,
                            added = s.lastModified,
                            categoryId = s.categoryId,
                            containerExtension = "series",
                            customSid = null,
                            directSource = null
                        )
                    }
                    .sortedByDescending { it.added.orEmpty() }

                val sports = loadSportsRow()
                val latestMovies = movies.take(12)
                val latestSeries = series.take(12)

                val rows = buildList {
                    if (latestMovies.isNotEmpty()) add(ContentRow("home_latest_movies", "Lo último agregado", latestMovies))
                    if (latestSeries.isNotEmpty()) add(ContentRow("home_latest_series", "Series recién actualizadas", latestSeries))
                    if (sports.isNotEmpty()) add(ContentRow("home_sports", "Eventos Deportivos del día", sports.take(12)))
                }

                val featured = (latestMovies + latestSeries + sports)
                    .filter { !it.streamIcon.isNullOrBlank() }
                    .firstOrNull()

                _state.value = MainState.Success(featured, rows)
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMovies() {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                val cats = repo.getVodCategories()
                val allItems = repo.getVodStreams()
                val rows = cats.mapNotNull { cat ->
                    val id = cat.id ?: return@mapNotNull null
                    val name = cat.name ?: return@mapNotNull null
                    val items = allItems.filter { it.categoryId == id }.take(30)
                    if (items.isEmpty()) null else ContentRow(id, name, items)
                }

                val featured = allItems
                    .filter { !it.streamIcon.isNullOrBlank() }
                    .maxByOrNull { it.rating?.toDoubleOrNull() ?: 0.0 }

                _state.value = MainState.Success(featured, rows)
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadSeries() {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                val cats = repo.getSeriesCategories()
                val allSeries = repo.getSeries()
                val allItems = allSeries.map { s ->
                    VodStream(
                        num = s.num,
                        name = s.name,
                        streamId = s.seriesId,
                        streamIcon = s.cover,
                        rating = s.rating,
                        rating5Based = s.rating5Based,
                        added = s.lastModified,
                        categoryId = s.categoryId,
                        containerExtension = "series",
                        customSid = null,
                        directSource = null
                    )
                }
                val rows = cats.mapNotNull { cat ->
                    val id = cat.id ?: return@mapNotNull null
                    val name = cat.name ?: return@mapNotNull null
                    val items = allItems.filter { it.categoryId == id }.take(30)
                    if (items.isEmpty()) null else ContentRow(id, name, items)
                }

                val featured = allItems
                    .filter { !it.streamIcon.isNullOrBlank() }
                    .maxByOrNull { it.rating?.toDoubleOrNull() ?: 0.0 }

                _state.value = MainState.Success(featured, rows)
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun loadSportsRow(): List<VodStream> {
        val categories = runCatching { repo.getLiveCategories() }.getOrElse { emptyList() }
        val sportsCategoryId = categories.firstOrNull { cat ->
            val name = cat.name.orEmpty().lowercase()
            name.contains("sport") ||
                name.contains("deport") ||
                name.contains("event") ||
                name.contains("tv || eventos deportivos del dia")
        }?.id ?: return emptyList()

        return runCatching { repo.getLiveStreams(sportsCategoryId) }.getOrElse { emptyList() }
            .map { stream ->
                VodStream(
                    num = stream.num,
                    name = stream.name,
                    streamId = stream.streamId,
                    streamIcon = stream.streamIcon,
                    rating = null,
                    rating5Based = null,
                    added = stream.added,
                    categoryId = stream.categoryId,
                    containerExtension = "live",
                    customSid = null,
                    directSource = null
                )
            }
            .take(30)
    }
}
