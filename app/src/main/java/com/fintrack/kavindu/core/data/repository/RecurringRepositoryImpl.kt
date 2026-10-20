package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.RecurringItemDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.firestoreWrite
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.RecurringItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringItemDao: RecurringItemDao,
    private val userFirestore: UserFirestore,
) : RecurringRepository {

    override fun observeRecurringItems(): Flow<List<RecurringItem>> =
        recurringItemDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun saveRecurringItem(item: RecurringItem): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.RECURRING_ITEMS).documentFor(item.id).set(item.toFirestoreMap())
    }

    override suspend fun deleteRecurringItem(id: String): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.RECURRING_ITEMS).document(id).delete()
    }
}
