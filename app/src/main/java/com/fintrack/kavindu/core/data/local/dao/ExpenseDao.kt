package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC, createdAt DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date >= :start ORDER BY date DESC, createdAt DESC")
    fun observeSince(start: LocalDate): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<ExpenseEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
