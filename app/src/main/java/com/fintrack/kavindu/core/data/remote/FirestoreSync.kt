package com.fintrack.kavindu.core.data.remote

import android.util.Log
import com.fintrack.kavindu.core.data.local.dao.BalanceCheckDao
import com.fintrack.kavindu.core.data.local.dao.CategoryDao
import com.fintrack.kavindu.core.data.local.dao.CryptoTransferDao
import com.fintrack.kavindu.core.data.local.dao.ExpenseDao
import com.fintrack.kavindu.core.data.local.dao.FreelanceProjectDao
import com.fintrack.kavindu.core.data.local.dao.GoalDao
import com.fintrack.kavindu.core.data.local.dao.IncomeDao
import com.fintrack.kavindu.core.data.local.dao.MilestoneDao
import com.fintrack.kavindu.core.data.local.dao.ProfileDao
import com.fintrack.kavindu.core.data.local.dao.RecurringItemDao
import com.fintrack.kavindu.core.data.repository.ErrorRepositoryImpl
import com.fintrack.kavindu.core.model.AppError
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Firestore real-time listeners. While a user is signed in, every change to their data in
 * Firestore (made on this phone, another phone, or the console) is copied into Room within moments,
 * even offline for changes made on this phone. Screens read Room, so they update by themselves.
 */
@Singleton
class FirestoreSync @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao,
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val recurringItemDao: RecurringItemDao,
    private val goalDao: GoalDao,
    private val freelanceProjectDao: FreelanceProjectDao,
    private val milestoneDao: MilestoneDao,
    private val cryptoTransferDao: CryptoTransferDao,
    private val balanceCheckDao: BalanceCheckDao,
    private val errorRepository: ErrorRepositoryImpl,
) {
    // A failed Room write must never crash the app: log it and show the user a calm message instead.
    private val crashGuard = CoroutineExceptionHandler { _, error ->
        Log.w(TAG, "Copying data into Room failed", error)
        errorRepository.report(AppError.SYNC_FAILED)
    }
    private var syncJob: Job? = null
    private var syncedUid: String? = null

    /** Starts copying this user's data into Room. Does nothing if it is already running for them. */
    fun start(uid: String) {
        if (syncedUid == uid && syncJob?.isActive == true) return
        syncJob?.cancel()

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job + crashGuard)
        syncJob = job
        syncedUid = uid

        val user = firestore.collection(FirestorePaths.USERS).document(uid)
        scope.launch { user.listenToDocument().collect { profileDao.replace(it?.toProfileEntity()) } }
        scope.sync(user.collection(FirestorePaths.INCOMES), { it.toIncomeEntity() }) { incomeDao.replaceAll(it) }
        scope.sync(user.collection(FirestorePaths.EXPENSES), { it.toExpenseEntity() }) { expenseDao.replaceAll(it) }
        scope.sync(user.collection(FirestorePaths.CATEGORIES), { it.toCategoryEntity() }) { categoryDao.replaceAll(it) }
        scope.sync(user.collection(FirestorePaths.RECURRING_ITEMS), { it.toRecurringItemEntity() }) {
            recurringItemDao.replaceAll(it)
        }
        scope.sync(user.collection(FirestorePaths.GOALS), { it.toGoalEntity() }) { goalDao.replaceAll(it) }
        scope.sync(user.collection(FirestorePaths.FREELANCE_PROJECTS), { it.toFreelanceProjectEntity() }) {
            freelanceProjectDao.replaceAll(it)
        }
        scope.sync(user.collection(FirestorePaths.MILESTONES), { it.toMilestoneEntity() }) {
            milestoneDao.replaceAll(it)
        }
        scope.sync(user.collection(FirestorePaths.CRYPTO_TRANSFERS), { it.toCryptoTransferEntity() }) {
            cryptoTransferDao.replaceAll(it)
        }
        scope.sync(user.collection(FirestorePaths.BALANCE_CHECKS), { it.toBalanceCheckEntity() }) {
            balanceCheckDao.replaceAll(it)
        }
    }

    /** Removes every listener and waits until the last Room write has finished (used before logout clears Room). */
    suspend fun stop() {
        syncJob?.cancelAndJoin()
        syncJob = null
        syncedUid = null
    }

    // One listener per collection. Each new snapshot replaces that Room table, in arrival order.
    private fun <T> CoroutineScope.sync(
        collection: CollectionReference,
        toEntity: (DocumentSnapshot) -> T?,
        save: suspend (List<T>) -> Unit,
    ) {
        launch { collection.listenToCollection(toEntity).collect { save(it) } }
    }

    private fun <T> CollectionReference.listenToCollection(toEntity: (DocumentSnapshot) -> T?): Flow<List<T>> =
        callbackFlow {
            val registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listener stopped for $path", error)
                    errorRepository.report(AppError.SYNC_FAILED)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot.documents.mapNotNull(toEntity))
            }
            awaitClose { registration.remove() }
        }.conflate() // only the newest snapshot matters, because it holds the whole collection

    private fun DocumentReference.listenToDocument(): Flow<DocumentSnapshot?> =
        callbackFlow {
            val registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listener stopped for $path", error)
                    errorRepository.report(AppError.SYNC_FAILED)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() })
            }
            awaitClose { registration.remove() }
        }.conflate()

    private companion object {
        const val TAG = "FirestoreSync"
    }
}
