package com.primetv.app.ui.live

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primetv.app.data.model.Category
import com.primetv.app.data.model.LiveStream
import com.primetv.app.data.repository.XtreamRepository
import kotlinx.coroutines.launch

sealed class LiveState {
    object Loading : LiveState()
    data class CategoriesLoaded(val categories: List<Category>) : LiveState()
    data class ChannelsLoaded(val channels: List<LiveStream>) : LiveState()
    data class Error(val message: String) : LiveState()
}

class LiveViewModel(private val repo: XtreamRepository) : ViewModel() {

    private val _state = MutableLiveData<LiveState>(LiveState.Loading)
    val state: LiveData<LiveState> = _state

    fun loadCategories() {
        _state.value = LiveState.Loading
        viewModelScope.launch {
            try {
                val cats = repo.getLiveCategories()
                _state.value = LiveState.CategoriesLoaded(cats)
                // Auto-load first category
                if (cats.isNotEmpty()) loadChannels(cats.first().id ?: return@launch)
            } catch (e: Exception) {
                _state.value = LiveState.Error(e.message ?: "Error")
            }
        }
    }

    fun loadChannels(categoryId: String?) {
        viewModelScope.launch {
            try {
                val channels = repo.getLiveStreams(categoryId)
                _state.value = LiveState.ChannelsLoaded(channels)
            } catch (e: Exception) {
                _state.value = LiveState.Error(e.message ?: "Error")
            }
        }
    }
}
