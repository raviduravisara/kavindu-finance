package com.fintrack.kavindu.core.model

// Firestore: users/{uid}/categories/{id}
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val sortOrder: Int,
    val createdAt: Long,
)
