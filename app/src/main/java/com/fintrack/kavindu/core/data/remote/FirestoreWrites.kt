package com.fintrack.kavindu.core.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference

// Saving does not wait for the server. Firestore keeps the change on the phone at once and sends it
// when online, so saving works offline and the listener puts the change into Room straight away.
// Waiting for the server would leave the Save button spinning for as long as the phone is offline.
inline fun firestoreWrite(write: () -> Task<*>): Result<Unit> =
    try {
        write().addOnFailureListener { Log.w("FirestoreWrite", "The server rejected a write", it) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

// A blank id means "new": Firestore makes a new document ID. Otherwise the existing document is replaced.
fun CollectionReference.documentFor(id: String): DocumentReference =
    if (id.isBlank()) document() else document(id)
