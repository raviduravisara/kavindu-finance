package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/balanceChecks/{id}
data class BalanceCheck(
    val id: String,
    val date: LocalDate,
    val bankBalanceCents: Long, // all bank accounts together
    val walletCashCents: Long,
    val isStartingPoint: Boolean, // true for the first check
    val expectedCents: Long? = null, // null on the first check
    val gapCents: Long? = null, // expected - actual
    val resultExpenseId: String? = null, // the "unrecorded spending" entry, if any
    val resultIncomeId: String? = null, // if a negative gap was added as income
    val createdAt: Long,
)
