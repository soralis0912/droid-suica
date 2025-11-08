package org.soralis.droid.suica

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.widget.Toast
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.soralis.droid.suica.databinding.ActivityMainBinding
import org.soralis.droid.suica.model.CardData
import org.soralis.droid.suica.service.NFCService
import org.soralis.droid.suica.ui.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var nfcService: NFCService
    
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    
    // NFC読み取りモードを制御するフラグ
    private var isReadingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        nfcService = NFCService(this)
        
        setupNFC()
        setupObservers()
        setupUI()
        
        // Handle intent if app was launched via NFC
        handleIntent(intent)
    }
    
    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            showError("This device doesn't support NFC.")
            return
        }
        
        if (!nfcAdapter!!.isEnabled) {
            showError("NFC is disabled.")
            return
        }
        
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        
        // Multiple intent filters for different NFC discovery types
        val tagDiscoveredFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val techDiscoveredFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val ndefDiscoveredFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        
        try {
            tagDiscoveredFilter.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            // Ignore if mime type fails
        }
        
        intentFiltersArray = arrayOf(
            tagDiscoveredFilter,
            techDiscoveredFilter,
            ndefDiscoveredFilter
        )
        
        techListsArray = arrayOf(
            arrayOf(NfcF::class.java.name),
            arrayOf("android.nfc.tech.IsoDep"),
            arrayOf("android.nfc.tech.NfcA"),
            arrayOf("android.nfc.tech.NfcB"),
            arrayOf("android.nfc.tech.NfcV")
        )
    }
    
    private fun setupObservers() {
        viewModel.cardData.observe(this) { cardData ->
            if (cardData != null) {
                displayCardData(cardData)
            }
        }
        
        viewModel.isReading.observe(this) { isReading ->
            updateReadingStatus(isReading)
        }
        
        viewModel.error.observe(this) { error ->
            if (error != null) {
                showError(error)
            }
        }
        
        viewModel.progress.observe(this) { progress ->
            updateProgress(progress)
        }
    }
    
    private fun setupUI() {
        binding.btnReadCard.setOnClickListener {
            hideError()
            isReadingEnabled = true
            enableForegroundDispatch()
            viewModel.startReading()
            updateStatus("カードをかざしてください")
        }
        
        binding.btnSettings.setOnClickListener {
            // Navigate to settings
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        updateStatus("ボタンを押してカード読み取りを開始してください")
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        
        // Log the action for debugging
        android.util.Log.d("MainActivity", "Received intent action: $action")
        
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            if (tag != null) {
                android.util.Log.d("MainActivity", "Tag detected: ${tag.id.joinToString("") { "%02X".format(it) }}")
                processNFCTag(tag)
            } else {
                android.util.Log.w("MainActivity", "Tag is null in intent")
            }
        } else {
            android.util.Log.d("MainActivity", "Not an NFC action: $action")
        }
    }
    
    private fun processNFCTag(tag: Tag) {
        android.util.Log.d("MainActivity", "Processing NFC tag")
        
        // タグの詳細情報を取得して表示
        val tagInfo = buildTagDebugInfo(tag)
        android.util.Log.d("MainActivity", "Tag Info:\n$tagInfo")
        
        // Display immediate feedback to user
        Toast.makeText(this, "カードを検出しました", Toast.LENGTH_SHORT).show()
        updateStatus("カードを処理中...")
        
        // タグ情報を画面に表示
        showTagInfo(tagInfo)
        
        lifecycleScope.launch {
            try {
                viewModel.readCard(tag)
                // 読み取り成功後、読み取りモードを無効化
                isReadingEnabled = false
                disableForegroundDispatch()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Card reading error", e)
                    // エラー時も読み取りモードを無効化
                    isReadingEnabled = false
                    disableForegroundDispatch()
                    if (e.message?.contains("Not a FeliCa card") == true) {
                        // FeliCa以外の場合は技術情報を強調表示
                        showError("FeliCaカードではありません。\n\n${e.message}\n\nタグ情報:\n$tagInfo")
                    } else {
                        showError("Card reading failed: ${e.message}\n\nタグ情報:\n$tagInfo")
                    }
            }
        }
    }
    
    private fun buildTagDebugInfo(tag: Tag): String {
        val sb = StringBuilder()
        
        // Tag ID
        val tagId = tag.id.joinToString("") { "%02X".format(it) }
        sb.append("Tag ID: $tagId\n")
        sb.append("Tag ID Length: ${tag.id.size} bytes\n\n")
        
        // Tech List
        sb.append("サポート技術:\n")
        tag.techList.forEach { tech ->
            sb.append("  - ${tech.substringAfterLast('.')}\n")
        }
        sb.append("\n")
        
        // NfcF specific info
        try {
            val nfcF = NfcF.get(tag)
            if (nfcF != null) {
                nfcF.connect()
                try {
                    sb.append("NFC-F 情報:\n")
                    sb.append("  Manufacturer: ${nfcF.manufacturer.joinToString("") { "%02X".format(it) }}\n")
                    sb.append("  System Code: ${nfcF.systemCode.joinToString("") { "%02X".format(it) }}\n")
                    sb.append("  Max Transceive Length: ${nfcF.maxTransceiveLength}\n")
                    sb.append("  Timeout: ${nfcF.timeout}ms\n")
                } finally {
                    nfcF.close()
                }
            } else {
                sb.append("NFC-F: 利用不可\n")
            }
        } catch (e: Exception) {
            sb.append("NFC-F エラー: ${e.message}\n")
        }
        
        return sb.toString()
    }
    
    private fun showTagInfo(tagInfo: String) {
        binding.apply {
            tvTagInfo.text = tagInfo
            scrollTagInfo.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun displayCardData(cardData: CardData) {
        hideError() // 成功時はエラーメッセージを隠す
        binding.apply {
            tvBalance.text = "残高: ¥${cardData.balance ?: "不明"}"
            tvCardType.text = "種別: ${cardData.cardType ?: "不明"}"
            tvCardId.text = "ID: ${cardData.idm ?: "不明"}"
            // Display PMm similarly to IDm
            tvPmm.text = "PMm: ${cardData.pmm ?: "不明"}"
            // Display raw server request/response JSON (if available)
            tvRawJson.text = cardData.rawJson ?: "サーバー情報なし"
            // Allow scrolling for long JSON
            tvRawJson.movementMethod = ScrollingMovementMethod()
            
            scrollCardInfo.visibility = android.view.View.VISIBLE
            
            // タグ情報は成功時も残しておく（デバッグ用）
        }
    }
    
    private fun updateReadingStatus(isReading: Boolean) {
        binding.apply {
            if (isReading) {
                progressBar.visibility = android.view.View.VISIBLE
                btnReadCard.isEnabled = false
                updateStatus("読み取り中...")
                hideError() // エラーメッセージを隠す
            } else {
                progressBar.visibility = android.view.View.GONE
                btnReadCard.isEnabled = true
                updateStatus("カードをかざしてください")
            }
        }
    }
    
    private fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
    }
    
    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
    }
    
    private fun showError(message: String) {
        binding.apply {
            tvError.text = message
            scrollError.visibility = android.view.View.VISIBLE
            updateStatus("エラーが発生しました")
        }
    }
    
    private fun hideError() {
        binding.apply {
            tvError.text = ""
            scrollError.visibility = android.view.View.GONE
        }
    }
    
    private fun hideTagInfo() {
        binding.apply {
            scrollTagInfo.visibility = android.view.View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // ボタンが押されている場合のみ有効化
        if (isReadingEnabled) {
            enableForegroundDispatch()
        }
    }
    
    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun enableForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }
    
    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
