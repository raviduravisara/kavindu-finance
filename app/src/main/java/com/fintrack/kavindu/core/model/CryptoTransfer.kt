package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/cryptoTransfers/{id}. Amounts in LKR.
data class CryptoTransfer(
    val id: String,
    val direction: CryptoDirection,
    val amountCents: Long,
    val date: LocalDate,
    val note: String? = null,
    val createdAt: Long,
)
