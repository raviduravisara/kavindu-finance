package com.fintrack.kavindu.core.data.remote

import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.AppException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code

// Turns Firebase and other exceptions into app errors, so screens never deal with Firebase classes.
fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    is FirebaseNetworkException -> AppError.NO_NETWORK
    is FirebaseFirestoreException -> when (code) {
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> AppError.NO_NETWORK
        Code.PERMISSION_DENIED -> AppError.SAVE_REJECTED
        Code.UNAUTHENTICATED -> AppError.NOT_SIGNED_IN
        Code.INVALID_ARGUMENT, Code.FAILED_PRECONDITION, Code.OUT_OF_RANGE -> AppError.INVALID_DATA
        else -> AppError.UNKNOWN
    }
    is IllegalArgumentException -> AppError.INVALID_DATA
    else -> AppError.UNKNOWN
}
