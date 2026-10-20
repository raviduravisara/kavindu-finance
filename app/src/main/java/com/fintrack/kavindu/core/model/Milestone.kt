package com.fintrack.kavindu.core.model

import java.time.LocalDate

// Firestore: users/{uid}/milestones/{id}. Flat (not under each project) so overdue
// milestones across all projects can be counted with one query.
data class Milestone(
    val id: String,
    val projectId: String,
    val title: String,
    val amountCents: Long,
    val currency: Currency,
    val dueDate: LocalDate,
    val status: MilestoneStatus = MilestoneStatus.PENDING,
    val incomeId: String? = null, // set when marked RECEIVED
    val createdAt: Long,
)
