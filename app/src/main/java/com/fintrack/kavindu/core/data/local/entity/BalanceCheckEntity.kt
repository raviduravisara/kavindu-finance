package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.BalanceCheck
import java.time.LocalDate

@Entity(tableName = "balance_checks", indices = [Index("date")])
data class BalanceCheckEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val bankBalanceCents: Long,
    val walletCashCents: Long,
    val isStartingPoint: Boolean,
    val expectedCents: Long?,
    val gapCents: Long?,
    val resultExpenseId: String?,
    val resultIncomeId: String?,
    val createdAt: Long,
)

fun BalanceCheckEntity.toModel() = BalanceCheck(
    id = id,
    date = date,
    bankBalanceCents = bankBalanceCents,
    walletCashCents = walletCashCents,
    isStartingPoint = isStartingPoint,
    expectedCents = expectedCents,
    gapCents = gapCents,
    resultExpenseId = resultExpenseId,
    resultIncomeId = resultIncomeId,
    createdAt = createdAt,
)

fun BalanceCheck.toEntity() = BalanceCheckEntity(
    id = id,
    date = date,
    bankBalanceCents = bankBalanceCents,
    walletCashCents = walletCashCents,
    isStartingPoint = isStartingPoint,
    expectedCents = expectedCents,
    gapCents = gapCents,
    resultExpenseId = resultExpenseId,
    resultIncomeId = resultIncomeId,
    createdAt = createdAt,
)
