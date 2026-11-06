package com.fintrack.kavindu.feature.auth.viewmodel

import android.content.Context
import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.model.AuthError
import com.fintrack.kavindu.core.model.asAuthError
import com.fintrack.kavindu.feature.auth.data.GoogleCredentialSource
import javax.inject.Inject

/**
 * "Continue with Google", shared by the Login and Register ViewModels: get the ID token from Google, then sign in
 * to Firebase with it. Returns the error to show, or null when it worked or he closed the picker (not an error).
 * On success the app switches to the signed-in screens by itself (SessionViewModel), as with email.
 */
class GoogleSignIn @Inject constructor(
    private val googleCredentialSource: GoogleCredentialSource,
    private val authRepository: AuthRepository,
) {
    suspend fun signIn(activityContext: Context): AuthError? {
        val token = googleCredentialSource.requestIdToken(activityContext)
        val result = token.fold(
            onSuccess = { idToken -> authRepository.signInWithGoogle(idToken) },
            onFailure = { Result.failure(it) },
        )
        val error = result.exceptionOrNull()?.asAuthError()
        return if (error == AuthError.GOOGLE_CANCELLED) null else error
    }
}
