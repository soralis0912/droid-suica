package com.example.droidsuica.service

import android.content.Context
import android.nfc.Tag
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class AuthClient(private val context: Context) {
    
    private val client = OkHttpClient()
    private var currentTag: Tag? = null
    
    companion object {
        private const val DEFAULT_SERVER_URL = "https://felica-auth.nyaa.ws"
        private const val CONTENT_TYPE_JSON = "application/json"
    }
    
    fun setCurrentTag(tag: Tag) {
        currentTag = tag
    }
    
    suspend fun mutualAuthentication(
        serverUrl: String = DEFAULT_SERVER_URL,
        bearerToken: String? = null,
        systemCode: Int,
        areas: List<Int>,
        services: List<Int>,
        idm: String,
        pmm: String
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        
        val requestBody = JSONObject().apply {
            put("system_code", systemCode)
            put("areas", areas)
            put("services", services)
            put("idm", idm)
            put("pmm", pmm)
        }
        val requestBodyString = requestBody.toString()
        
        val request = Request.Builder()
            .url("$serverUrl/api/mutual_authentication")
            .post(requestBody.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
            .apply {
                if (bearerToken != null) {
                    addHeader("Authorization", "Bearer $bearerToken")
                }
            }
            .build()
        
    val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Authentication failed: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")
        
        // Parse JSON response
        val jsonResponse = JSONObject(responseBody)

        // Convert to Map for easier handling and include request/raw response
        val result = mutableMapOf<String, Any>()
        jsonResponse.keys().forEach { key ->
            result[key] = jsonResponse.get(key)
        }

        // Include the original request body and raw response for UI/debugging
        result["request"] = requestBodyString
        result["raw_response"] = responseBody

        result
    }
    
    suspend fun readBlocks(
        serverUrl: String = DEFAULT_SERVER_URL,
        bearerToken: String? = null,
        serviceCode: Int,
        blockNumbers: List<Int>
    ): List<ByteArray> = withContext(Dispatchers.IO) {
        
        val requestBody = JSONObject().apply {
            put("service_code", serviceCode)
            put("block_numbers", blockNumbers)
        }
        
        val request = Request.Builder()
            .url("$serverUrl/api/read_blocks")
            .post(requestBody.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
            .apply {
                if (bearerToken != null) {
                    addHeader("Authorization", "Bearer $bearerToken")
                }
            }
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Block read failed: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")
        
        val jsonResponse = JSONObject(responseBody)
        val blocksArray = jsonResponse.getJSONArray("blocks")
        
        val blocks = mutableListOf<ByteArray>()
        for (i in 0 until blocksArray.length()) {
            val blockHex = blocksArray.getString(i)
            val blockBytes = blockHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            blocks.add(blockBytes)
        }
        
        blocks
    }
}
