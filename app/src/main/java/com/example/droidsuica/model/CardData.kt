package com.example.droidsuica.model

data class CardData(
    val idm: String? = null,
    val pmm: String? = null,
    val systemCode: String? = null,
    val balance: Int? = null,
    val cardType: String? = null,
    val ownerName: String? = null,
    val issuedAt: String? = null,
    val expiresAt: String? = null,
    val history: List<TransactionHistory> = emptyList(),
    val commuterPass: CommuterPass? = null,
    val rawJson: String? = null
)

data class TransactionHistory(
    val date: String? = null,
    val time: String? = null,
    val transactionType: String? = null,
    val entryStation: String? = null,
    val exitStation: String? = null,
    val amount: Int? = null,
    val balance: Int? = null
)

data class CommuterPass(
    val validFrom: String? = null,
    val validTo: String? = null,
    val startStation: String? = null,
    val endStation: String? = null
)

data class NFCCardData(
    val idm: String,
    val pmm: String,
    val systemCode: Int
)
