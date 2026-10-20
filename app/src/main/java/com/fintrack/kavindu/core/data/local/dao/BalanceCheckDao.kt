package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.BalanceCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceCheckDao {

    @Query("SELECT * FROM balance_checks ORDER BY date DESC, createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<BalanceCheckEntity?>

    @Upsert
    suspend fun upsertAll(items: List<BalanceCheckEntity>)

    @Query("DELETE FROM balance_checks")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<BalanceCheckEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
