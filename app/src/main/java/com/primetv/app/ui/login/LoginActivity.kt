package com.primetv.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.primetv.app.App
import com.primetv.app.BuildConfig
import com.primetv.app.data.repository.XtreamRepository
import com.primetv.app.databinding.ActivityLoginBinding
import com.primetv.app.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private val prefs get() = App.instance.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (prefs.hasCredentials()) { goToMain(); return }

        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"

        val repo = XtreamRepository(prefs, App.instance.db)
        viewModel = ViewModelProvider(this, LoginViewModelFactory(repo))[LoginViewModel::class.java]

        observeState()
        setupInputs()
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showSplash()
                is LoginState.Success -> {
                    // Save credentials (already saved on first login; update on re-login)
                    val server = binding.etServer.text.toString().ifBlank { prefs.serverUrl }
                    val user   = binding.etUsername.text.toString().ifBlank { prefs.username }
                    val pass   = binding.etPassword.text.toString().ifBlank { prefs.password }
                    prefs.serverUrl = server
                    prefs.username  = user
                    prefs.password  = pass
                    goToMain()
                }
                is LoginState.Error -> {
                    showForm()
                    showError(state.message)
                }
                else -> {}
            }
        }
    }

    private fun setupInputs() {
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnLogin.setOnFocusChangeListener { _, _ -> }
    }

    private fun attemptLogin() {
        hideError()
        viewModel.login(
            binding.etServer.text.toString(),
            binding.etUsername.text.toString(),
            binding.etPassword.text.toString()
        )
    }

    private fun showSplash() {
        binding.splashLayout.visibility = View.VISIBLE
        binding.loginForm.visibility = View.GONE
    }

    private fun showForm() {
        binding.splashLayout.visibility = View.GONE
        binding.loginForm.visibility = View.VISIBLE
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
