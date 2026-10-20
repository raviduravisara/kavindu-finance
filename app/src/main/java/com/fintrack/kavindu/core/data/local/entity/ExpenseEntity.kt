package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import java.time.LocalDate

@Entity(tableName = "expenses", indices = [Index("date")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val currency: Currency,
    val date: LocalDate,
    val categoryId: String,
    val categoryType: CategoryType,
    val channel: String,
    val account: String,
    val note: String?,
    val origin: ExpenseOrigin,
    val recurringItemId: String?,
    val isConfirmed: Boolean,
    val createdAt: Long,
)

fun ExpenseEntity.toModel() = Expense(
    id = id,
    amountCents = amountCents,
    currency = currency,
    date = date,
    categoryId = categoryId,
    categoryType = categoryType,
    channel = channel,
    account = account,
    note = note,
    origin = origin,
    recurringItemId = recurringItemId,
    isConfirmed = isConfirmed,
    createdAt = createdAt,
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amountCents = amountCents,
    currency = currency,
    date = date,
    categoryId = categoryId,
    categoryType = categoryType,
    channel = channel,
    account = account,
    note = note,
    origin = origin,
    recurringItemId = recurringItemId,
    isConfirmed = isConfirmed,
    createdAt = createdAt,
)
