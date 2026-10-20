package com.fintrack.kavindu.core.model

// Firestore: users/{uid}/freelanceProjects/{id}
data class FreelanceProject(
    val id: String,
    val clientName: String,
    val projectName: String,
    val createdAt: Long,
)
