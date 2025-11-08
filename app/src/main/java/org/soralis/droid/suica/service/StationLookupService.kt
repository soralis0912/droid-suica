package org.soralis.droid.suica.service

import android.content.Context
import org.soralis.droid.suica.model.*
import java.io.BufferedReader
import java.io.InputStreamReader

class StationLookupService(private val context: Context) {
    
    private val stationMap = mutableMapOf<String, String>()
    
    init {
        loadStationCodes()
    }
    
    private fun loadStationCodes() {
        try {
            val inputStream = context.assets.open("station_codes.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            reader.useLines { lines ->
                lines.drop(1) // Skip header
                    .forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val lineCode = parts[0].trim()
                            val stationCode = parts[1].trim()
                            val companyName = parts[2].trim()
                            val stationName = parts[3].trim()
                            
                            val key = "${lineCode}_${stationCode}"
                            val value = if (companyName.isNotEmpty()) {
                                "$companyName $stationName"
                            } else {
                                stationName
                            }
                            stationMap[key] = value
                        }
                    }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
    
    fun getStationName(lineCode: Int, stationCode: Int): String? {
        val key = "${lineCode}_${stationCode}"
        return stationMap[key]
    }
    
    fun formatStation(lineCode: Int, stationCode: Int): String {
        return getStationName(lineCode, stationCode) ?: "不明な駅 ($lineCode-$stationCode)"
    }
}
