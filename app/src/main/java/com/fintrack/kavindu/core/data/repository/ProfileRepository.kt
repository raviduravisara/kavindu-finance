package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    suspend fun updateCycleStartDay(day: Int): Result<Unit>

    suspend fun updateLastChannelAndAccount(channel: String, account: String): Result<Unit>
}
