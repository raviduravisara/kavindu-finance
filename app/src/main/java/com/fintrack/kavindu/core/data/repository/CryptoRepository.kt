package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.CryptoTransfer
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {
    fun observeTransfers(): Flow<List<CryptoTransfer>>

    suspend fun saveTransfer(transfer: CryptoTransfer): Result<Unit>

    suspend fun deleteTransfer(id: String): Result<Unit>
}
