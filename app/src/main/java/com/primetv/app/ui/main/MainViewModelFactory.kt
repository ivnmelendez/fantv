package com.primetv.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.primetv.app.data.repository.XtreamRepository

class MainViewModelFactory(private val repo: XtreamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repo) as T
    }
}
