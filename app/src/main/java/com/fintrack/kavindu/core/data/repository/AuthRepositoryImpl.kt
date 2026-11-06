package com.fintrack.kavindu.core.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.fintrack.kavindu.core.data.local.AppDatabase
import com.fintrack.kavindu.core.data.remote.FirestorePaths
import com.fintrack.kavindu.core.data.remote.FirestoreSync
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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase,
    private val firestoreSync: FirestoreSync,
) : AuthRepository {

    // Set only after the user's profile exists, so the app never opens for a user without one.
    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid)
    override val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        // Still signed in from last time: start copying their data into Room straight away.
        auth.currentUser?.uid?.let(firestoreSync::start)
    }

    override suspend fun register(email: String, password: String): Result<Unit> = runAuthCall {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.UNKNOWN)
        // Firebase's own copy of the email, exactly as the security rules see it.
        finishSignIn(user.uid, user.email ?: email)
    }

    override suspend fun login(email: String, password: String): Result<Unit> = runAuthCall {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.UNKNOWN)
        finishSignIn(user.uid, user.email ?: email)
    }

    // Firebase checks the token with Google. The same account (same uid, same data) is used every time.
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runAuthCall {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: throw AuthException(AuthError.GOOGLE_FAILED)
        finishSignIn(user.uid, user.email ?: throw AuthException(AuthError.GOOGLE_FAILED))
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runAuthCall {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun logout() {
        firestoreSync.stop() // no listener may write into Room after it is cleared
        auth.signOut()
        _currentUserId.value = null
        // Room holds a copy of this user's data; clear it so the next user cannot see it.
        withContext(Dispatchers.IO) { database.clearAllTables() }
        // Forget the Google account chosen last time, so the account picker shows again on the next sign-in.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nothing to clear (for example an email user, or no Google Play services): logout still completes.
        }
    }

    // The profile is created on first register. Login checks again, in case that first write failed.
    private suspend fun finishSignIn(uid: String, email: String) {
        try {
            createProfileIfMissing(uid, email)
        } catch (e: Exception) {
            auth.signOut() // never leave a signed-in user without a profile
            throw e
        }
        firestoreSync.start(uid)
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
