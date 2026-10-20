package com.fintrack.kavindu.feature.auth.viewmodel

import com.fintrack.kavindu.core.model.AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    private val validEmail = "name@example.com"

    @Test
    fun validateEmail_rejectsEmptyAndMalformed() {
        assertEquals(AuthError.EMAIL_REQUIRED, AuthValidator.validateEmail(""))
        assertEquals(AuthError.EMAIL_REQUIRED, AuthValidator.validateEmail("   "))
        assertEquals(AuthError.INVALID_EMAIL, AuthValidator.validateEmail("name"))
        assertEquals(AuthError.INVALID_EMAIL, AuthValidator.validateEmail("name@example"))
        assertEquals(AuthError.INVALID_EMAIL, AuthValidator.validateEmail("na me@example.com"))
    }

    @Test
    fun validateEmail_acceptsValidEmailWithSpacesAround() {
        assertNull(AuthValidator.validateEmail(validEmail))
        assertNull(AuthValidator.validateEmail("  $validEmail  "))
    }

    @Test
    fun validateLogin_needsAPassword() {
        assertEquals(AuthError.PASSWORD_REQUIRED, AuthValidator.validateLogin(validEmail, ""))
        assertNull(AuthValidator.validateLogin(validEmail, "anything"))
    }

    @Test
    fun validateLogin_checksEmailFirst() {
        assertEquals(AuthError.EMAIL_REQUIRED, AuthValidator.validateLogin("", ""))
    }

    @Test
    fun validateRegister_rejectsShortPassword() {
        assertEquals(AuthError.WEAK_PASSWORD, AuthValidator.validateRegister(validEmail, "12345", "12345"))
    }

    @Test
    fun validateRegister_rejectsDifferentPasswords() {
        assertEquals(
            AuthError.PASSWORDS_DO_NOT_MATCH,
            AuthValidator.validateRegister(validEmail, "secret123", "secret124"),
        )
    }

    @Test
    fun validateRegister_acceptsValidForm() {
        assertNull(AuthValidator.validateRegister(validEmail, "secret123", "secret123"))
    }
}
