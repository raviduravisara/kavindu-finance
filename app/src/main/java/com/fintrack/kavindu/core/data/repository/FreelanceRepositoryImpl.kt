package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.FreelanceProjectDao
import com.fintrack.kavindu.core.data.local.dao.MilestoneDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.FreelanceProject
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreelanceRepositoryImpl @Inject constructor(
    private val freelanceProjectDao: FreelanceProjectDao,
    private val milestoneDao: MilestoneDao,
    private val userFirestore: UserFirestore,
) : FreelanceRepository {

    override fun observeProjects(): Flow<List<FreelanceProject>> =
        freelanceProjectDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun observeMilestones(projectId: String): Flow<List<Milestone>> =
        milestoneDao.observeForProject(projectId).map { rows -> rows.map { it.toModel() } }

    override fun observeOverdueCount(today: LocalDate): Flow<Int> = milestoneDao.observeOverdueCount(today)

    override suspend fun saveProject(project: FreelanceProject): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.FREELANCE_PROJECTS).documentFor(project.id)
            .set(project.toFirestoreMap())
    }

    override suspend fun saveMilestone(milestone: Milestone): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.MILESTONES).documentFor(milestone.id).set(milestone.toFirestoreMap())
    }

    // The income entry and the milestone change are saved in one batch, so a payment always matches its project.
    override suspend fun markMilestoneReceived(milestoneId: String, income: Income): Result<Unit> = userFirestore.write {
        val incomeRef = userFirestore.collection(FirestorePaths.INCOMES).documentFor(income.id)
        val milestoneRef = userFirestore.collection(FirestorePaths.MILESTONES).document(milestoneId)
        userFirestore.batch()
            .set(incomeRef, income.copy(milestoneId = milestoneId).toFirestoreMap())
            .update(milestoneRef, mapOf("status" to MilestoneStatus.RECEIVED.name, "incomeId" to incomeRef.id))
            .commit()
    }
}
