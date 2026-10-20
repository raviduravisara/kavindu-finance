package com.fintrack.kavindu.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.model.AuthError
import com.fintrack.kavindu.core.model.asAuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }

    fun onConfirmPasswordChange(confirmPassword: String) =
        _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) }

    fun onPasswordVisibilityToggle() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onRegisterClicked() {
        val state = _uiState.value
        if (state.isLoading) return

        val invalid = AuthValidator.validateRegister(state.email, state.password, state.confirmPassword)
        if (invalid != null) {
            _uiState.update { it.copy(error = invalid) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the profile exists and the app switches to the signed-in screens by itself.
            val result = authRepository.register(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.asAuthError()) }
        }
    }
}
