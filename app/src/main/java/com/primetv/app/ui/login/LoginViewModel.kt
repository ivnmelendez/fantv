package com.primetv.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primetv.app.data.model.AuthResponse
import com.primetv.app.data.repository.XtreamRepository
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val auth: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val repo: XtreamRepository) : ViewModel() {

    private val _state = MutableLiveData<LoginState>(LoginState.Idle)
    val state: LiveData<LoginState> = _state

    fun login(server: String, username: String, password: String) {
        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("All fields are required")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val result = repo.authenticate(server.trim(), username.trim(), password.trim())
                if (result.userInfo?.auth == 1) {
                    _state.value = LoginState.Success(result)
                } else {
                    _state.value = LoginState.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Connection error: ${e.message}")
            }
        }
    }
}
