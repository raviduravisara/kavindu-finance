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

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val error: AuthError? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun onSendClicked() {
        val state = _uiState.value
        if (state.isLoading) return

        val invalid = AuthValidator.validateEmail(state.email)
        if (invalid != null) {
            _uiState.update { it.copy(error = invalid) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(state.email.trim())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEmailSent = result.isSuccess,
                    error = result.exceptionOrNull()?.asAuthError(),
                )
            }
        }
    }
}
