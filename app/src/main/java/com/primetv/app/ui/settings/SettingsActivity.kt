package com.primetv.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.primetv.app.BuildConfig
import com.primetv.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }
}
