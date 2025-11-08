package com.example.droidsuica.service

import android.content.Context
import android.nfc.Tag
import com.example.droidsuica.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CardParserService(
    private val context: Context,
    private val authClient: AuthClient,
    private val stationLookup: StationLookupService
) {
    
    companion object {
        private const val SYSTEM_CODE = 0x0003
        private val AREA_NODE_IDS = listOf(0x0000, 0x0040, 0x0800, 0x0FC0, 0x1000)
        private val SERVICE_NODE_IDS = listOf(
            0x0048, 0x0088, 0x0810, 0x08C8, 0x090C,
            0x1008, 0x1048, 0x108C, 0x10C8
        )
    }
    
    suspend fun parseCard(
        tag: Tag,
        nfcCardData: NFCCardData,
        serverUrl: String,
        bearerToken: String?
    ): CardData = withContext(Dispatchers.IO) {
        
        // Set the current tag for auth client
        authClient.setCurrentTag(tag)
        
        try {
            // Perform mutual authentication
            val authResult = authClient.mutualAuthentication(
                serverUrl = serverUrl,
                bearerToken = bearerToken,
                systemCode = SYSTEM_CODE,
                areas = AREA_NODE_IDS,
                services = SERVICE_NODE_IDS,
                idm = nfcCardData.idm,
                pmm = nfcCardData.pmm
            )
            
            // Read card data blocks
            val cardInfo = readCardInformation(serverUrl, bearerToken)
            val history = readTransactionHistory(serverUrl, bearerToken)
            
            // Parse the data
            CardData(
                idm = nfcCardData.idm,
                pmm = nfcCardData.pmm,
                systemCode = nfcCardData.systemCode.toString(16),
                balance = cardInfo.balance,
                cardType = cardInfo.cardType,
                ownerName = cardInfo.ownerName,
                issuedAt = cardInfo.issuedAt,
                expiresAt = cardInfo.expiresAt,
                history = history,
                commuterPass = cardInfo.commuterPass,
                rawJson = createRawJson(authResult, cardInfo, history)
            )
            
        } catch (e: Exception) {
            // Return basic card data if detailed parsing fails
            CardData(
                idm = nfcCardData.idm,
                pmm = nfcCardData.pmm,
                systemCode = nfcCardData.systemCode.toString(16),
                rawJson = "{\"error\": \"${e.message}\"}"
            )
        }
    }
    
    private suspend fun readCardInformation(serverUrl: String, bearerToken: String?): CardInfo {
        // Read attribute information (service code 2)
        val attributeBlocks = authClient.readBlocks(serverUrl, bearerToken, 2, listOf(0))
        
        val balance = if (attributeBlocks.isNotEmpty()) {
            val block = attributeBlocks[0]
            if (block.size >= 2) {
                // Balance is in the first 2 bytes (little endian)
                (block[1].toInt() and 0xFF shl 8) or (block[0].toInt() and 0xFF)
            } else null
        } else null
        
        // Read issue information (service code 1)
        val issueBlocks = authClient.readBlocks(serverUrl, bearerToken, 1, listOf(0))
        
        return CardInfo(
            balance = balance,
            cardType = determineCardType(balance),
            ownerName = null, // Would need additional parsing
            issuedAt = null,  // Would need additional parsing
            expiresAt = null, // Would need additional parsing
            commuterPass = null // Would need additional parsing
        )
    }
    
    private suspend fun readTransactionHistory(serverUrl: String, bearerToken: String?): List<TransactionHistory> {
        val historyList = mutableListOf<TransactionHistory>()
        
        try {
            // Read history blocks (service code 8)
            val historyBlocks = authClient.readBlocks(serverUrl, bearerToken, 8, (0..19).toList())
            
            historyBlocks.forEachIndexed { index, block ->
                if (block.size >= 16) {
                    val history = parseHistoryBlock(block)
                    if (history != null) {
                        historyList.add(history)
                    }
                }
            }
        } catch (e: Exception) {
            // History reading failed, return empty list
        }
        
        return historyList
    }
    
    private fun parseHistoryBlock(block: ByteArray): TransactionHistory? {
        return try {
            if (block.size < 16) return null
            
            // Parse transaction type
            val transactionType = when (block[0].toInt() and 0xFF) {
                1 -> "鉄道"
                2 -> "バス"
                3 -> "商品購入"
                4 -> "チャージ"
                else -> "不明"
            }
            
            // Parse date (bytes 4-5, big endian)
            val dateValue = (block[4].toInt() and 0xFF shl 8) or (block[5].toInt() and 0xFF)
            val date = formatDate(dateValue)
            
            // Parse time (bytes 6-7)
            val timeValue = (block[6].toInt() and 0xFF shl 8) or (block[7].toInt() and 0xFF)
            val time = formatTime(timeValue)
            
            // Parse stations
            val entryLineCode = block[8].toInt() and 0xFF
            val entryStationCode = block[9].toInt() and 0xFF
            val exitLineCode = block[10].toInt() and 0xFF
            val exitStationCode = block[11].toInt() and 0xFF
            
            val entryStation = if (entryLineCode != 0 && entryStationCode != 0) {
                stationLookup.formatStation(entryLineCode, entryStationCode)
            } else null
            
            val exitStation = if (exitLineCode != 0 && exitStationCode != 0) {
                stationLookup.formatStation(exitLineCode, exitStationCode)
            } else null
            
            // Parse balance (bytes 14-15, little endian)
            val balance = (block[15].toInt() and 0xFF shl 8) or (block[14].toInt() and 0xFF)
            
            TransactionHistory(
                date = date,
                time = time,
                transactionType = transactionType,
                entryStation = entryStation,
                exitStation = exitStation,
                balance = balance
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun determineCardType(balance: Int?): String {
        return when {
            balance != null -> "Suica"
            else -> "不明"
        }
    }
    
    private fun formatDate(dateValue: Int): String {
        return try {
            val baseDate = Calendar.getInstance().apply {
                set(2000, 0, 1) // January 1, 2000
                add(Calendar.DAY_OF_YEAR, dateValue)
            }
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(baseDate.time)
        } catch (e: Exception) {
            "不明"
        }
    }
    
    private fun formatTime(timeValue: Int): String {
        return try {
            val hours = (timeValue shr 11) and 0x1F
            val minutes = (timeValue shr 5) and 0x3F
            String.format("%02d:%02d", hours, minutes)
        } catch (e: Exception) {
            "不明"
        }
    }
    
    private fun createRawJson(authResult: Map<String, Any>, cardInfo: CardInfo, history: List<TransactionHistory>): String {
        return try {
            JSONObject().apply {
                put("auth_result", JSONObject(authResult))
                put("balance", cardInfo.balance)
                put("card_type", cardInfo.cardType)
                put("history_count", history.size)
                put("history", history.map { h ->
                    JSONObject().apply {
                        put("date", h.date)
                        put("time", h.time)
                        put("type", h.transactionType)
                        put("entry_station", h.entryStation)
                        put("exit_station", h.exitStation)
                        put("balance", h.balance)
                    }
                })
            }.toString(2)
        } catch (e: Exception) {
            "{\"error\": \"Failed to create JSON: ${e.message}\"}"
        }
    }
    
    private data class CardInfo(
        val balance: Int?,
        val cardType: String?,
        val ownerName: String?,
        val issuedAt: String?,
        val expiresAt: String?,
        val commuterPass: CommuterPass?
    )
}
