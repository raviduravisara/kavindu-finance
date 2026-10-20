package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestones WHERE projectId = :projectId ORDER BY dueDate")
    fun observeForProject(projectId: String): Flow<List<MilestoneEntity>>

    // Overdue = due date has passed and still not paid.
    @Query("SELECT COUNT(*) FROM milestones WHERE status = 'PENDING' AND dueDate < :today")
    fun observeOverdueCount(today: LocalDate): Flow<Int>

    @Upsert
    suspend fun upsertAll(items: List<MilestoneEntity>)

    @Query("DELETE FROM milestones")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<MilestoneEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
