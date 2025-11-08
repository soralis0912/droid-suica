package com.example.droidsuica.ui

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.droidsuica.model.CardData
import com.example.droidsuica.service.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val nfcService = NFCService(application)
    private val authClient = AuthClient(application)
    private val stationLookup = StationLookupService(application)
    private val cardParser = CardParserService(application, authClient, stationLookup)
    private val settingsManager = SettingsManager(application)
    
    private val _cardData = MutableLiveData<CardData?>()
    val cardData: LiveData<CardData?> = _cardData
    
    private val _isReading = MutableLiveData<Boolean>()
    val isReading: LiveData<Boolean> = _isReading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress
    
    fun startReading() {
        _isReading.value = true
        _error.value = null
        _progress.value = 0
    }
    
    fun readCard(tag: Tag) {
        viewModelScope.launch {
            try {
                _isReading.value = true
                _error.value = null
                _progress.value = 10
                
                // Read basic NFC data
                val nfcCardData = nfcService.readCard(tag)
                _progress.value = 30
                
                // Parse detailed card data
                val cardData = cardParser.parseCard(
                    tag = tag,
                    nfcCardData = nfcCardData,
                    serverUrl = settingsManager.getAuthServerUrl(),
                    bearerToken = settingsManager.getAuthToken()
                )
                _progress.value = 100
                
                _cardData.value = cardData
                _isReading.value = false
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _isReading.value = false
                _progress.value = 0
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun resetCardData() {
        _cardData.value = null
        _progress.value = 0
    }
}
