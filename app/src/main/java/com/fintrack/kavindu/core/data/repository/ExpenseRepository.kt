package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.util.PayCycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ExpenseRepository {
    fun observeExpenses(cycle: PayCycle): Flow<List<Expense>>

    /** Used by the goal and the balance check. */
    fun observeExpensesSince(date: LocalDate): Flow<List<Expense>>

    /** Adds (blank id), edits, or undoes a delete (saving the same expense again with the same id). */
    suspend fun saveExpense(expense: Expense): Result<Unit>

    suspend fun deleteExpense(id: String): Result<Unit>
}
