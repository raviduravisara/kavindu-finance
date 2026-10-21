package com.fintrack.kavindu.core.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference

// A blank id means "new": Firestore makes a new document ID. Otherwise the existing document is replaced.
fun CollectionReference.documentFor(id: String): DocumentReference =
    if (id.isBlank()) document() else document(id)
