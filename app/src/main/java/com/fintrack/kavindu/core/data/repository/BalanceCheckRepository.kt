package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Income
import kotlinx.coroutines.flow.Flow

interface BalanceCheckRepository {
    fun observeLatestCheck(): Flow<BalanceCheck?>

    /**
     * Saves the check together with, at most, one result entry: the "unrecorded spending" expense
     * (positive gap) or the income the user chose to add (negative gap).
     */
    suspend fun saveCheck(check: BalanceCheck, unrecordedSpending: Expense?, foundIncome: Income?): Result<Unit>
}
