package com.fintrack.kavindu.core.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user's uid, or null when signed out. */
    val currentUserId: Flow<String?>

    /** Also creates the profile document and the starter categories. */
    suspend fun register(email: String, password: String): Result<Unit>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Also clears Room, so the next user cannot see this user's data. */
    suspend fun logout()
}
