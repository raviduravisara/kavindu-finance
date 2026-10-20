package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.FreelanceProject
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.Milestone
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FreelanceRepository {
    fun observeProjects(): Flow<List<FreelanceProject>>

    fun observeMilestones(projectId: String): Flow<List<Milestone>>

    /** Milestones still PENDING after their due date, for the dashboard. */
    fun observeOverdueCount(today: LocalDate): Flow<Int>

    suspend fun saveProject(project: FreelanceProject): Result<Unit>

    suspend fun saveMilestone(milestone: Milestone): Result<Unit>

    /** Marks the milestone RECEIVED and saves its income entry together, in one write. */
    suspend fun markMilestoneReceived(milestoneId: String, income: Income): Result<Unit>
}
