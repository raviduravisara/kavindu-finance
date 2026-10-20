package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.CryptoTransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoTransferDao {

    @Query("SELECT * FROM crypto_transfers ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<CryptoTransferEntity>>

    @Upsert
    suspend fun upsertAll(items: List<CryptoTransferEntity>)

    @Query("DELETE FROM crypto_transfers")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<CryptoTransferEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
