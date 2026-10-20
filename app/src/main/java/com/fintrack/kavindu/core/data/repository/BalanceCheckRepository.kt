package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.Expense
import kotlinx.coroutines.flow.Flow

interface BalanceCheckRepository {
    fun observeLatestCheck(): Flow<BalanceCheck?>

    /** Saves the check and, if there is a positive gap, its "unrecorded spending" expense. */
    suspend fun saveCheck(check: BalanceCheck, unrecordedSpending: Expense?): Result<Unit>
}
