package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes WHERE date BETWEEN :start AND :end ORDER BY date DESC, createdAt DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date >= :start ORDER BY date DESC, createdAt DESC")
    fun observeSince(start: LocalDate): Flow<List<IncomeEntity>>

    @Upsert
    suspend fun upsertAll(items: List<IncomeEntity>)

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<IncomeEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
