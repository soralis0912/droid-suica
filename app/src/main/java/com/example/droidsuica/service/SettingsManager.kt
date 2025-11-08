package com.example.droidsuica.service

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "droid_suica_settings"
        private const val KEY_AUTH_SERVER_URL = "auth_server_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val DEFAULT_AUTH_SERVER_URL = "https://felica-auth.nyaa.ws"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getAuthServerUrl(): String {
        return prefs.getString(KEY_AUTH_SERVER_URL, DEFAULT_AUTH_SERVER_URL) ?: DEFAULT_AUTH_SERVER_URL
    }
    
    fun setAuthServerUrl(url: String) {
        prefs.edit().putString(KEY_AUTH_SERVER_URL, url).apply()
    }
    
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun setAuthToken(token: String?) {
        if (token == null) {
            prefs.edit().remove(KEY_AUTH_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        }
    }
}
