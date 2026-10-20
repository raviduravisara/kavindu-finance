package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.RecurringItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringItemDao {

    @Query("SELECT * FROM recurring_items ORDER BY dayOfMonth, name")
    fun observeAll(): Flow<List<RecurringItemEntity>>

    @Upsert
    suspend fun upsertAll(items: List<RecurringItemEntity>)

    @Query("DELETE FROM recurring_items")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<RecurringItemEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
