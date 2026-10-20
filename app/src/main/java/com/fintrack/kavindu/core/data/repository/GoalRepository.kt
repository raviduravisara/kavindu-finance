package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    /** The one active goal, or null if none is set yet. */
    fun observeGoal(): Flow<Goal?>

    suspend fun saveGoal(goal: Goal): Result<Unit>

    suspend fun deleteGoal(id: String): Result<Unit>
}
