package com.primetv.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primetv.app.App
import com.primetv.app.data.model.Category
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

class MainViewModel(private v?al repo: XtreamRepository) : ViewModel() {

    private val tmdb get() = App.instance.tmdb

    private val _state = MutableLiveData<MainState>(MainState.Loading)
    val state: LiveData<MainState> = _state

    private val _featuredInfo = MutableLiveData<FeaturedInfo?>()
    val featuredInfo: LiveData<FeaturedInfo?> = _featuredInfo

    private val infoCache = HashMap<Int, FeaturedInfo>()
    private val pendingFetches = HashSet<Int>()
    private var currentFeaturedId = -1

    private fun isCamCategoryName(name: String): Boolean {
        val normalized = name.trim().lowercase()
        return normalized == "cam" ||
            normalized.contains(" cam") ||
            normalized.contains("cam ") ||
            normalized.contains("vod cam")
    }

    private fun isYearCategoryName(name: String): Boolean {
        return Regex("""\b(19|20)\d{2}\b""").containsMatchIn(name)
    }

    private fun isPlatformCategoryName(name: String): Boolean {
        val normalized = name.trim().lowercase()
        val keywords = listOf(
            "netflix",
            "disney",
            "prime video",
            "primevideo",
            "amazon prime",
            "amazon",
            "hbo",
            "max",
            "paramount",
            "apple tv",
            "apple tv+",
            "showtime",
            "sky",
            "movistar",
            "crunchyroll",
            "universal",
            "peacock",
            "tubi",
            "mubi",
            "starz",
            "star+",
            "star plus"
        )
        return keywords.any { keyword -> normalized.contains(keyword) }
    }

    private fun isGenreCategoryName(name: String): Boolean {
        val normalized = name.trim().lowercase()
        val keywords = listOf(
            "accion",
            "acción",
            "action",
            "aventura",
            "adventure",
            "animacion",
            "animación",
            "animation",
            "anime",
            "comedia",
            "comedy",
            "crimen",
            "crime",
            "documental",
            "documentary",
            "drama",
            "familia",
            "family",
            "fantasia",
            "fantasía",
            "fantasy",
            "historia",
            "history",
            "horror",
            "terror",
            "misterio",
            "mystery",
            "musical",
            "romance",
            "romantic",
            "ciencia ficcion",
            "ciencia ficción",
            "sci-fi",
            "sci fi",
            "thriller",
            "suspenso",
            "western",
            "guerra",
            "war",
            "biografia",
            "biografía",
            "biopic",
            "infantil",
            "kids",
            "deporte",
            "sport",
            "sports"
        )
        return keywords.any { keyword -> normalized.contains(keyword) }
    }

    private fun categorySortBucket(name: String): Int = when {
        isGenreCategoryName(name) -> 0
        isPlatformCategoryName(name) -> 1
        isYearCategoryName(name) -> 2
        else -> 3
    }

    private fun seriesCategorySortBucket(name: String): Int = when {
        isPlatformCategoryName(name) -> 0
        else -> 1
    }

    private fun displayCategoryName(name: String): String {
        val trimmed = name.trim()
        val cleaned = trimmed.replaceFirst(
            Regex("^(vod|series)\\s*(\\|\\|)?\\s*[:\\-|]*\\s*", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        return cleaned.ifBlank { trimmed }
    }

    fun loadFeaturedInfo(streamId: Int, name: String, isSeries: Boolean) {
        currentFeaturedId = streamId
        infoCache[streamId]?.let { cached ->
            _featuredInfo.value = cached
            return
        }
        if (pendingFetches.contains(streamId)) return
        pendingFetches.add(streamId)
        viewModelScope.launch {
            delay(200)
            if (currentFeaturedId != streamId) {
                pendingFetches.remove(streamId)
                return@launch
            }
            val result = tmdb.search(name, isSeries)
            val info = result?.let {
                FeaturedInfo(
                    plot = it.overview,
                    genre = tmdb.genreNames(it.genreIds, isSeries),
                    cast = null,
                    releaseDate = it.releaseDate ?: it.firstAirDate,
                    rating = it.voteAverage?.let { v -> String.format("%.1f", v) },
                    seasonCount = null,
                    backdropPath = tmdb.backdropUrl(it.backdropPath)
                )
            }
            pendingFetches.remove(streamId)
            info?.let { infoCache[streamId] = it }
            if (currentFeaturedId == streamId) _featuredInfo.value = info
        }
    }

    fun loadHome() {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                val vodCategories = runCatching { repo.getVodCategories() }.getOrElse { emptyList() }
                val camCategoryIds = vodCategories
                    .filter { it.name?.let(::isCamCategoryName) == true }
                    .mapNotNull { it.id }
                    .toSet()

                val movies = runCatching { repo.getVodStreams() }.getOrElse { emptyList() }
                    .filter { it.categoryId !in camCategoryIds }
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
                val filteredCats = cats.filterNot { cat -> cat.name?.let(::isCamCategoryName) == true }
                    .sortedWith(
                        compareBy<Category> { categorySortBucket(it.name.orEmpty()) }
                            .thenBy { it.name.orEmpty().lowercase() }
                    )
                val camCategoryIds = cats
                    .filter { it.name?.let(::isCamCategoryName) == true }
                    .mapNotNull { it.id }
                    .toSet()
                val allItems = repo.getVodStreams()
                    .filter { it.categoryId !in camCategoryIds }
                val rows = filteredCats.mapNotNull { cat ->
                    val id = cat.id ?: return@mapNotNull null
                    val name = cat.name?.let(::displayCategoryName) ?: return@mapNotNull null
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
                val rows = cats
                    .sortedWith(
                        compareBy<Category> { seriesCategorySortBucket(it.name.orEmpty()) }
                            .thenBy { it.name.orEmpty().lowercase() }
                    )
                    .mapNotNull { cat ->
                        val id = cat.id ?: return@mapNotNull null
                        val name = cat.name?.let(::displayCategoryName) ?: return@mapNotNull null
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
