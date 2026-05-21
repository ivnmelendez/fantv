package com.primetv.app.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.primetv.app.data.repository.XtreamRepository

class LiveViewModelFactory(private val repo: XtreamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LiveViewModel(repo) as T
    }
}
