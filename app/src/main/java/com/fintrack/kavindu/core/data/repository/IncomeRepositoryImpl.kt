package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.IncomeDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.firestoreWrite
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.util.PayCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao,
    private val userFirestore: UserFirestore,
) : IncomeRepository {

    override fun observeIncomes(cycle: PayCycle): Flow<List<Income>> =
        incomeDao.observeBetween(cycle.start, cycle.end).map { rows -> rows.map { it.toModel() } }

    override fun observeIncomesSince(date: LocalDate): Flow<List<Income>> =
        incomeDao.observeSince(date).map { rows -> rows.map { it.toModel() } }

    override suspend fun saveIncome(income: Income): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.INCOMES).documentFor(income.id).set(income.toFirestoreMap())
    }

    override suspend fun deleteIncome(id: String): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.INCOMES).document(id).delete()
    }
}
