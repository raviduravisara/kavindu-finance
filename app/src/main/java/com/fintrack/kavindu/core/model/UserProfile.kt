package com.fintrack.kavindu.core.model

import com.fintrack.kavindu.core.util.PayCycle

// Firestore: users/{uid}. Created on first register with the default settings.
data class UserProfile(
    val id: String,
    val email: String,
    val cycleStartDay: Int = PayCycle.DEFAULT_START_DAY,
    val baseCurrency: Currency = Currency.LKR,
    val lastChannel: String? = null,
    val lastAccount: String? = null,
    val createdAt: Long,
)
