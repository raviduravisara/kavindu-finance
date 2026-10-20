package com.fintrack.kavindu.core.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton

// Gives the repositories the signed-in user's place in Firestore: users/{uid} and its collections.
@Singleton
class UserFirestore @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    fun userDocument(): DocumentReference {
        val uid = auth.currentUser?.uid ?: error("No user is signed in")
        return firestore.collection(FirestorePaths.USERS).document(uid)
    }

    fun collection(name: String): CollectionReference = userDocument().collection(name)

    fun batch(): WriteBatch = firestore.batch()
}
