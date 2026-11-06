package com.fintrack.kavindu.core.model

// Why a sign-in step failed, in app terms. Screens turn each value into a message.
enum class AuthError {
    EMAIL_REQUIRED,
    INVALID_EMAIL,
    PASSWORD_REQUIRED,
    WEAK_PASSWORD,
    PASSWORDS_DO_NOT_MATCH,
    WRONG_CREDENTIALS,
    EMAIL_IN_USE,
    NO_NETWORK,
    TOO_MANY_ATTEMPTS,
    GOOGLE_CANCELLED, // he closed Google's account picker: not an error, never shown
    NO_GOOGLE_ACCOUNT, // no Google account on this phone
    GOOGLE_FAILED, // Google sign-in did not complete
    UNKNOWN,
}

// Carried inside a failed Result from AuthRepository.
class AuthException(val error: AuthError) : Exception(error.name)

fun Throwable.asAuthError(): AuthError = (this as? AuthException)?.error ?: AuthError.UNKNOWN
