package com.fintrack.kavindu.core.model

// Why loading or saving data failed, in app terms. Every screen turns it into a friendly message
// with messageRes() (core/ui/components/ErrorMessages.kt).
enum class AppError {
    NO_NETWORK,
    NOT_SIGNED_IN,
    SAVE_REJECTED, // the server refused a save (security rules), so Firestore undid it
    INVALID_DATA,
    SYNC_FAILED, // data could not be loaded or copied into the phone's database
    UNKNOWN,
}

// Carried inside a failed Result from any data repository.
class AppException(val error: AppError) : Exception(error.name)

fun Throwable.asAppError(): AppError = (this as? AppException)?.error ?: AppError.UNKNOWN
