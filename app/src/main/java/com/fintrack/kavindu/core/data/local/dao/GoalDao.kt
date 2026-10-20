package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    // One active goal: the newest one.
    @Query("SELECT * FROM goals ORDER BY createdAt DESC LIMIT 1")
    fun observeGoal(): Flow<GoalEntity?>

    @Upsert
    suspend fun upsertAll(items: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<GoalEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
