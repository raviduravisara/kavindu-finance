package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/goals/{id}. One active goal.
data class Goal(
    val id: String,
    val name: String,
    val targetCents: Long,
    val deadline: LocalDate,
    val startingSavingsCents: Long,
    val startDate: LocalDate, // "saved so far" counts from here
    val createdAt: Long,
)
