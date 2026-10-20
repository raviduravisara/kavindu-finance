package com.fintrack.kavindu.core.data.repository

import com.fintrack.kavindu.core.data.local.AppDatabase
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.model.AuthError
import com.fintrack.kavindu.core.model.AuthException
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.StarterData
import com.fintrack.kavindu.core.util.PayCycle
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase,
) : AuthRepository {

    // Set only after the user's profile exists, so the app never opens for a user without one.
    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid)
    override val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    override suspend fun register(email: String, password: String): Result<Unit> = runAuthCall {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.UNKNOWN)
        finishSignIn(user.uid, email)
    }

    override suspend fun login(email: String, password: String): Result<Unit> = runAuthCall {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.UNKNOWN)
        finishSignIn(user.uid, user.email ?: email)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runAuthCall {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUserId.value = null
        // Room holds a copy of this user's data; clear it so the next user cannot see it.
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    // The profile is created on first register. Login checks again, in case that first write failed.
    private suspend fun finishSignIn(uid: String, email: String) {
        try {
            createProfileIfMissing(uid, email)
        } catch (e: Exception) {
            auth.signOut() // never leave a signed-in user without a profile
            throw e
        }
        _currentUserId.value = uid
    }

    private suspend fun createProfileIfMissing(uid: String, email: String) {
        val userDoc = firestore.collection(FirestorePaths.USERS).document(uid)
        if (userDoc.get().await().exists()) return

        val now = System.currentTimeMillis()
        val batch = firestore.batch() // profile and starter categories are saved together, or not at all
        batch.set(
            userDoc,
            mapOf(
                "email" to email,
                "cycleStartDay" to PayCycle.DEFAULT_START_DAY,
                "baseCurrency" to Currency.LKR.name,
                "lastChannel" to null,
                "lastAccount" to null,
                "createdAt" to now,
            ),
        )
        StarterData.categories.forEachIndexed { index, (name, type) ->
            batch.set(
                userDoc.collection(FirestorePaths.CATEGORIES).document(),
                mapOf("name" to name, "type" to type.name, "sortOrder" to index, "createdAt" to now),
            )
        }
        batch.commit().await()
    }

    private suspend fun runAuthCall(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AuthException(e.toAuthError()))
        }

    private fun Exception.toAuthError(): AuthError = when (this) {
        is AuthException -> error
        is FirebaseNetworkException -> AuthError.NO_NETWORK
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_ATTEMPTS
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD // must come before its parent class below
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.WRONG_CREDENTIALS
        is FirebaseAuthInvalidUserException -> AuthError.WRONG_CREDENTIALS
        is FirebaseFirestoreException ->
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthError.NO_NETWORK else AuthError.UNKNOWN
        else -> AuthError.UNKNOWN
    }
}
