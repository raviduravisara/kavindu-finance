package com.fintrack.kavindu.core.data.remote

import android.util.Log
import com.fintrack.kavindu.core.data.repository.ErrorRepositoryImpl
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.AppException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton

// Gives the repositories the signed-in user's place in Firestore (users/{uid} and its collections)
// and one safe way to save.
@Singleton
class UserFirestore @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val errorRepository: ErrorRepositoryImpl,
) {
    fun userDocument(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw AppException(AppError.NOT_SIGNED_IN)
        return firestore.collection(FirestorePaths.USERS).document(uid)
    }

    fun collection(name: String): CollectionReference = userDocument().collection(name)

    fun batch(): WriteBatch = firestore.batch()

    /**
     * Runs one save. It does not wait for the server: Firestore keeps the change on the phone at once
     * and sends it when online, so saving works offline and the listener puts it into Room straight away.
     * If the server later rejects it (security rules), Firestore undoes it and the user sees a message.
     */
    fun write(save: () -> Task<*>): Result<Unit> =
        try {
            save().addOnFailureListener { error ->
                Log.w(TAG, "The server rejected a save", error)
                errorRepository.report(error.toAppError())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppException(e.toAppError()))
        }

    private companion object {
        const val TAG = "UserFirestore"
    }
}
