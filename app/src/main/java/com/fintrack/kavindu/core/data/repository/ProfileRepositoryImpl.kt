package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.dao.ProfileDao
import com.fintrack.kavindu.core.data.local.entity.toModel
import com.fintrack.kavindu.core.data.remote.UserFirestore
import com.fintrack.kavindu.core.data.remote.firestoreWrite
import com.fintrack.kavindu.core.model.UserProfile
import com.fintrack.kavindu.core.util.PayCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val userFirestore: UserFirestore,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observeProfile().map { it?.toModel() }

    override suspend fun updateCycleStartDay(day: Int): Result<Unit> {
        if (!PayCycle.isValidStartDay(day)) {
            return Result.failure(IllegalArgumentException("Start day must be between 1 and 28"))
        }
        return firestoreWrite { userFirestore.userDocument().update("cycleStartDay", day) }
    }

    override suspend fun updateLastChannelAndAccount(channel: String, account: String): Result<Unit> =
        firestoreWrite {
            userFirestore.userDocument().update(mapOf("lastChannel" to channel, "lastAccount" to account))
        }
}
