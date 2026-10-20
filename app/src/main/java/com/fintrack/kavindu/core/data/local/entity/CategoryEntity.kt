package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CategoryType,
    val sortOrder: Int,
    val createdAt: Long,
)

fun CategoryEntity.toModel() = Category(
    id = id,
    name = name,
    type = type,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    type = type,
    sortOrder = sortOrder,
    createdAt = createdAt,
)
