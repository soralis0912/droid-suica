package com.example.droidsuica

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.droidsuica.databinding.ActivitySettingsBinding
import com.example.droidsuica.service.SettingsManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        settingsManager = SettingsManager(this)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        // ツールバーの戻るボタン
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "設定"
        
        // 保存ボタンのクリックリスナー
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        // リセットボタンのクリックリスナー
        binding.btnReset.setOnClickListener {
            resetToDefault()
        }
    }
    
    private fun loadSettings() {
        binding.etAuthServerUrl.setText(settingsManager.getAuthServerUrl())
        binding.etAuthToken.setText(settingsManager.getAuthToken() ?: "")
    }
    
    private fun saveSettings() {
        val authServerUrl = binding.etAuthServerUrl.text.toString().trim()
        val authToken = binding.etAuthToken.text.toString().trim()
        
        if (authServerUrl.isEmpty()) {
            binding.etAuthServerUrl.error = "認証サーバーURLは必須です"
            return
        }
        
        if (!isValidUrl(authServerUrl)) {
            binding.etAuthServerUrl.error = "有効なURLを入力してください"
            return
        }
        
        settingsManager.setAuthServerUrl(authServerUrl)
        settingsManager.setAuthToken(authToken.ifEmpty { null })
        
        Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun resetToDefault() {
        binding.etAuthServerUrl.setText("https://felica-auth.nyaa.ws")
        binding.etAuthToken.setText("")
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlPattern = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
            urlPattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
