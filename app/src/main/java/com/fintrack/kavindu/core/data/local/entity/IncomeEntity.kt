package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import java.time.LocalDate

@Entity(tableName = "incomes", indices = [Index("date")])
data class IncomeEntity(
    @PrimaryKey val id: String,
    val source: IncomeSource,
    val amountCents: Long,
    val currency: Currency,
    val lkrAmountCents: Long,
    val rateCentsPerUnit: Long,
    val date: LocalDate,
    val note: String?,
    val projectId: String?,
    val milestoneId: String?,
    val createdAt: Long,
)

fun IncomeEntity.toModel() = Income(
    id = id,
    source = source,
    amountCents = amountCents,
    currency = currency,
    lkrAmountCents = lkrAmountCents,
    rateCentsPerUnit = rateCentsPerUnit,
    date = date,
    note = note,
    projectId = projectId,
    milestoneId = milestoneId,
    createdAt = createdAt,
)

fun Income.toEntity() = IncomeEntity(
    id = id,
    source = source,
    amountCents = amountCents,
    currency = currency,
    lkrAmountCents = lkrAmountCents,
    rateCentsPerUnit = rateCentsPerUnit,
    date = date,
    note = note,
    projectId = projectId,
    milestoneId = milestoneId,
    createdAt = createdAt,
)
