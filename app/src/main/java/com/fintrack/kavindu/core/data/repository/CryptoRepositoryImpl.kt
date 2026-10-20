package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.CryptoTransferDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.documentFor
import com.fintrack.kavindu.core.data.remote.firestoreWrite
import com.fintrack.kavindu.core.data.remote.toFirestoreMap
import com.fintrack.kavindu.core.model.CryptoTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val cryptoTransferDao: CryptoTransferDao,
    private val userFirestore: UserFirestore,
) : CryptoRepository {

    override fun observeTransfers(): Flow<List<CryptoTransfer>> =
        cryptoTransferDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun saveTransfer(transfer: CryptoTransfer): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.CRYPTO_TRANSFERS).documentFor(transfer.id)
            .set(transfer.toFirestoreMap())
    }

    override suspend fun deleteTransfer(id: String): Result<Unit> = firestoreWrite {
        userFirestore.collection(FirestorePaths.CRYPTO_TRANSFERS).document(id).delete()
    }
}
