package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.BalanceCheckDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceCheckRepositoryImpl @Inject constructor(
    private val balanceCheckDao: BalanceCheckDao,
    private val userFirestore: UserFirestore,
) : BalanceCheckRepository {

    override fun observeLatestCheck(): Flow<BalanceCheck?> =
        balanceCheckDao.observeLatest().map { it?.toModel() }

    // The check and its "unrecorded spending" expense are saved together in one batch.
    override suspend fun saveCheck(check: BalanceCheck, unrecordedSpending: Expense?): Result<Unit> = userFirestore.write {
        val batch = userFirestore.batch()
        val checkRef = userFirestore.collection(FirestorePaths.BALANCE_CHECKS).documentFor(check.id)
        var savedCheck = check
        if (unrecordedSpending != null) {
            val expenseRef = userFirestore.collection(FirestorePaths.EXPENSES).documentFor(unrecordedSpending.id)
            batch.set(expenseRef, unrecordedSpending.toFirestoreMap())
            savedCheck = check.copy(resultExpenseId = expenseRef.id)
        }
        batch.set(checkRef, savedCheck.toFirestoreMap())
        batch.commit()
    }
}
