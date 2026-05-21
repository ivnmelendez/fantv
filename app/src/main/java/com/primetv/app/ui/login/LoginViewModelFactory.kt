package com.primetv.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.primetv.app.data.repository.XtreamRepository

class LoginViewModelFactory(private val repo: XtreamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(repo) as T
    }
}
