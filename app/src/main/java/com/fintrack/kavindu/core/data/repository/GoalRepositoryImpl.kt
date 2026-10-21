package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.GoalDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val userFirestore: UserFirestore,
) : GoalRepository {

    override fun observeGoal(): Flow<Goal?> = goalDao.observeGoal().map { it?.toModel() }

    override suspend fun saveGoal(goal: Goal): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.GOALS).documentFor(goal.id).set(goal.toFirestoreMap())
    }

    override suspend fun deleteGoal(id: String): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.GOALS).document(id).delete()
    }
}
