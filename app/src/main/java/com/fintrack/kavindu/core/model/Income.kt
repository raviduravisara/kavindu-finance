package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/incomes/{id}
data class Income(
    val id: String,
    val source: IncomeSource,
    val amountCents: Long, // in the original currency
    val currency: Currency,
    val lkrAmountCents: Long, // what actually reached the bank
    val rateCentsPerUnit: Long, // LKR cents per 1 unit of currency, worked out once on save
    val date: LocalDate,
    val note: String? = null,
    val projectId: String? = null, // set when created from a freelance milestone
    val milestoneId: String? = null,
    val createdAt: Long,
)
