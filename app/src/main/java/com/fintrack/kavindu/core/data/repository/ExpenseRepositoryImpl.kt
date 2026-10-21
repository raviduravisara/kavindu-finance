package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.ExpenseDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.util.PayCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val userFirestore: UserFirestore,
) : ExpenseRepository {

    override fun observeExpenses(cycle: PayCycle): Flow<List<Expense>> =
        expenseDao.observeBetween(cycle.start, cycle.end).map { rows -> rows.map { it.toModel() } }

    override fun observeExpensesSince(date: LocalDate): Flow<List<Expense>> =
        expenseDao.observeSince(date).map { rows -> rows.map { it.toModel() } }

    // A recurring entry arrives with its fixed id, e.g. "rent_2026-10-25", so it is never saved twice.
    override suspend fun saveExpense(expense: Expense): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.EXPENSES).documentFor(expense.id).set(expense.toFirestoreMap())
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = userFirestore.write {
        userFirestore.collection(FirestorePaths.EXPENSES).document(id).delete()
    }
}
