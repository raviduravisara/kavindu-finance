package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.util.PayCycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IncomeRepository {
    fun observeIncomes(cycle: PayCycle): Flow<List<Income>>

    /** Used by the goal and the balance check. */
    fun observeIncomesSince(date: LocalDate): Flow<List<Income>>

    /** Adds a new income or replaces the one with the same id. */
    suspend fun saveIncome(income: Income): Result<Unit>

    suspend fun deleteIncome(id: String): Result<Unit>
}
