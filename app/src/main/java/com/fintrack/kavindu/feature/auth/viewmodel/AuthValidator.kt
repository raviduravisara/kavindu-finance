package com.fintrack.kavindu.feature.auth.viewmodel

import com.fintrack.kavindu.core.model.AuthError

// Checks the form before anything is sent to Firebase. Plain Kotlin, so it is unit tested.
object AuthValidator {

    const val MIN_PASSWORD_LENGTH = 6 // Firebase's own minimum

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(email: String): AuthError? = when {
        email.isBlank() -> AuthError.EMAIL_REQUIRED
        !emailPattern.matches(email.trim()) -> AuthError.INVALID_EMAIL
        else -> null
    }

    fun validateLogin(email: String, password: String): AuthError? =
        validateEmail(email) ?: if (password.isEmpty()) AuthError.PASSWORD_REQUIRED else null

    fun validateRegister(email: String, password: String, confirmPassword: String): AuthError? =
        validateEmail(email) ?: when {
            password.isEmpty() -> AuthError.PASSWORD_REQUIRED
            password.length < MIN_PASSWORD_LENGTH -> AuthError.WEAK_PASSWORD
            password != confirmPassword -> AuthError.PASSWORDS_DO_NOT_MATCH
            else -> null
        }
}
