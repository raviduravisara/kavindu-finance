package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.RecurringItem
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun observeRecurringItems(): Flow<List<RecurringItem>>

    suspend fun saveRecurringItem(item: RecurringItem): Result<Unit>

    suspend fun deleteRecurringItem(id: String): Result<Unit>
}
