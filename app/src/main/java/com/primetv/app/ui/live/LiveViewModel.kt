package com.primetv.app.ui.live

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primetv.app.data.model.LiveStream
import com.primetv.app.data.repository.XtreamRepository
import kotlinx.coroutines.launch

sealed class LiveState {
    object Loading : LiveState()
    object Ready : LiveState()
    data class Error(val message: String) : LiveState()
}

class LiveViewModel(private val repo: XtreamRepository) : ViewModel() {

    private val _state = MutableLiveData<LiveState>(LiveState.Loading)
    val state: LiveData<LiveState> = _state

    private val _categories = MutableLiveData<List<Pair<String, String>>>()
    val categories: LiveData<List<Pair<String, String>>> = _categories

    private val _channels = MutableLiveData<List<LiveStream>>()
    val channels: LiveData<List<LiveStream>> = _channels

    var allChannels: List<LiveStream> = emptyList()
        private set

    fun loadAll() {
        _state.value = LiveState.Loading
        viewModelScope.launch {
            try {
                var cats = repo.getLiveCategories()
                var streams = repo.getLiveStreams()
                if (cats.isEmpty() || streams.isEmpty()) {
                    repo.syncLive()
                    cats = repo.getLiveCategories()
                    streams = repo.getLiveStreams()
                }
                allChannels = streams
                val catPairs = cats.mapNotNull { cat ->
                    val name = cat.name ?: return@mapNotNull null
                    val id = cat.id ?: return@mapNotNull null
                    name to id
                }
                _categories.value = catPairs
                val firstId = cats.firstOrNull()?.id
                _channels.value = if (firstId != null)
                    allChannels.filter { it.categoryId == firstId }
                else allChannels
                _state.value = LiveState.Ready
            } catch (e: Exception) {
                _state.value = LiveState.Error(e.message ?: "Error")
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _channels.value = allChannels.filter { it.categoryId == categoryId }
    }
}
