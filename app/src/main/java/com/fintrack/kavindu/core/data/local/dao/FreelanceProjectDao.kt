package com.fintrack.kavindu.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fintrack.kavindu.core.data.local.entity.FreelanceProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FreelanceProjectDao {

    @Query("SELECT * FROM freelance_projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FreelanceProjectEntity>>

    @Upsert
    suspend fun upsertAll(items: List<FreelanceProjectEntity>)

    @Query("DELETE FROM freelance_projects")
    suspend fun deleteAll()

    // Used by the Firestore listener: the table becomes an exact copy of Firestore.
    @Transaction
    suspend fun replaceAll(items: List<FreelanceProjectEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
