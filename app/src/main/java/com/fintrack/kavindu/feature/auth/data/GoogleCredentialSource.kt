package com.fintrack.kavindu.feature.auth.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.AuthError
import com.fintrack.kavindu.core.model.AuthException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Asks Credential Manager (the current Google sign-in API; the old GoogleSignInClient is deprecated) for a Google
 * ID token. Firebase Auth then checks that token in AuthRepository.signInWithGoogle.
 *
 * Google's account picker is shown over the app, so this needs the screen's Activity. The ViewModel passes it
 * straight in for this one call and never keeps it.
 */
class GoogleCredentialSource @Inject constructor() {

    suspend fun requestIdToken(activityContext: Context): Result<String> {
        // The web client ID comes from google-services.json (the Google Services plugin turns it into this string).
        val option = GetSignInWithGoogleOption.Builder(activityContext.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(AuthException(AuthError.GOOGLE_FAILED))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Result.failure(AuthException(AuthError.GOOGLE_CANCELLED))
        } catch (e: NoCredentialException) {
            Result.failure(AuthException(AuthError.NO_GOOGLE_ACCOUNT))
        } catch (e: GetCredentialException) {
            Result.failure(AuthException(AuthError.GOOGLE_FAILED))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(AuthException(AuthError.GOOGLE_FAILED))
        }
    }
}
