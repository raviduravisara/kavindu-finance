package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.RecurringItem
import java.time.LocalDate

@Entity(tableName = "recurring_items")
data class RecurringItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String,
    val dayOfMonth: Int,
    val isEstimate: Boolean,
    val isSubscription: Boolean,
    val isActive: Boolean,
    val lastReviewedDate: LocalDate?,
    val createdAt: Long,
)

fun RecurringItemEntity.toModel() = RecurringItem(
    id = id,
    name = name,
    amountCents = amountCents,
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    isEstimate = isEstimate,
    isSubscription = isSubscription,
    isActive = isActive,
    lastReviewedDate = lastReviewedDate,
    createdAt = createdAt,
)

fun RecurringItem.toEntity() = RecurringItemEntity(
    id = id,
    name = name,
    amountCents = amountCents,
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    isEstimate = isEstimate,
    isSubscription = isSubscription,
    isActive = isActive,
    lastReviewedDate = lastReviewedDate,
    createdAt = createdAt,
)
