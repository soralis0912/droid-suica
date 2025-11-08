package com.example.droidsuica.service

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.NfcF
import com.example.droidsuica.model.NFCCardData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NFCService(private val context: Context) {
    
    companion object {
        private const val SYSTEM_CODE = 0x0003
        private val AREA_NODE_IDS = listOf(0x0000, 0x0040, 0x0800, 0x0FC0, 0x1000)
        private val SERVICE_NODE_IDS = listOf(
            0x0048, 0x0088, 0x0810, 0x08C8, 0x090C,
            0x1008, 0x1048, 0x108C, 0x10C8
        )
    }
    
    suspend fun readCard(tag: Tag): NFCCardData = withContext(Dispatchers.IO) {
        val nfcF = NfcF.get(tag) 
        if (nfcF == null) {
            val availableTechs = tag.techList.joinToString(", ") { it.substringAfterLast('.') }
            throw Exception("Not a FeliCa card. Available techs: $availableTechs")
        }
        
        try {
            nfcF.connect()
            
            // Get IDm from tag
            val idm = tag.id
            
            // For now, we'll use a simple approach to get basic card info
            // In a real implementation, you would need to send proper FeliCa commands
            val idmHex = idm.joinToString("") { "%02X".format(it) }
            
            // Log manufacturer info
            val manufacturerHex = nfcF.manufacturer.joinToString("") { "%02X".format(it) }
            android.util.Log.d("NFCService", "Manufacturer: $manufacturerHex")
            
            // Try to retrieve PMm via FeliCa Polling command.
            // If this fails, fall back to a zeroed PMm string.
            var pmmHex = "0000000000000000"
            try {
                // Polling command format: [LEN][CMD=0x00][SYSTEM_CODE(2)][REQUEST_CODE][TIME_SLOT]
                // LEN = 6 (CMD + SYSTEM_CODE(2) + REQUEST_CODE + TIME_SLOT + LEN)
                val sysHigh = ((SYSTEM_CODE shr 8) and 0xFF).toByte()
                val sysLow = (SYSTEM_CODE and 0xFF).toByte()
                val pollingCmd = byteArrayOf(
                    0x06.toByte(), // length
                    0x00.toByte(), // Polling command
                    sysHigh,
                    sysLow,
                    0x00.toByte(), // Request code
                    0x00.toByte()  // Time slot
                )

                android.util.Log.d("NFCService", "Sending polling command: ${pollingCmd.joinToString("") { "%02X".format(it) }}")
                val resp = nfcF.transceive(pollingCmd)
                android.util.Log.d("NFCService", "Polling response: ${resp.joinToString("") { "%02X".format(it) }}")

                // Expected response: [LEN][RESP_CODE=0x01][IDm(8)][PMm(8)]...
                if (resp != null && resp.size >= 18 && (resp[1].toInt() and 0xFF) == 0x01) {
                    val pmmBytes = resp.sliceArray(10 until 18)
                    pmmHex = pmmBytes.joinToString("") { "%02X".format(it) }
                    android.util.Log.d("NFCService", "PMm extracted: $pmmHex")
                } else {
                    android.util.Log.w("NFCService", "Unexpected polling response format or length: ${resp?.size ?: 0}")
                }
            } catch (e: Exception) {
                // If transceive/polling fails, keep placeholder PMm and continue.
                android.util.Log.e("NFCService", "Polling command failed", e)
                throw Exception("FeliCa polling failed: ${e.message}. This may not be a mobile Suica card.")
            }
            
            NFCCardData(
                idm = idmHex,
                pmm = pmmHex,
                systemCode = SYSTEM_CODE
            )
        } catch (e: Exception) {
            android.util.Log.e("NFCService", "Error reading card", e)
            throw e
        } finally {
            try {
                nfcF.close()
            } catch (e: Exception) {
                // Ignore close exceptions
                android.util.Log.w("NFCService", "Error closing NFC connection", e)
            }
        }
    }
    
    suspend fun exchangeCommand(tag: Tag, command: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val nfcF = NfcF.get(tag) ?: throw Exception("Not a FeliCa card")
        
        try {
            nfcF.connect()
            val response = nfcF.transceive(command)
            
            if (response.isEmpty()) {
                throw Exception("Empty response from card")
            }
            
            response
        } finally {
            try {
                nfcF.close()
            } catch (e: Exception) {
                // Ignore close exceptions
            }
        }
    }
}
