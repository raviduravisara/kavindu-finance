package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/recurringItems/{id}
data class RecurringItem(
    val id: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String,
    val dayOfMonth: Int, // 1-28, the day it is due
    val isEstimate: Boolean = false, // utilities: added unconfirmed
    val isSubscription: Boolean = false, // listed in subscription review
    val isActive: Boolean = true,
    val lastReviewedDate: LocalDate? = null,
    val createdAt: Long,
)
