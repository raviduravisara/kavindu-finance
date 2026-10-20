package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/expenses/{id}. Recurring entries use the fixed ID "<recurringItemId>_<date>".
data class Expense(
    val id: String,
    val amountCents: Long,
    val currency: Currency = Currency.LKR,
    val date: LocalDate,
    val categoryId: String,
    val categoryType: CategoryType, // copied on save, so old cycles keep their totals
    val channel: String,
    val account: String,
    val note: String? = null,
    val origin: ExpenseOrigin = ExpenseOrigin.MANUAL,
    val recurringItemId: String? = null,
    val isConfirmed: Boolean = true, // false for a utility estimate until confirmed
    val createdAt: Long,
)
