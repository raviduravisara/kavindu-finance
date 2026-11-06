package com.fintrack.kavindu.feature.auth.viewmodel

import android.content.Context
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: AuthError? = null,
) {
    /** While either sign-in runs, the form and both buttons are disabled. */
    val isBusy: Boolean get() = isLoading || isGoogleLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignIn: GoogleSignIn,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }

    fun onPasswordVisibilityToggle() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    /** [activityContext] shows Google's account picker; it is used for this call only and never stored. */
    fun onGoogleClicked(activityContext: Context) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isGoogleLoading = true, error = null) }
        viewModelScope.launch {
            val error = googleSignIn.signIn(activityContext)
            _uiState.update { it.copy(isGoogleLoading = false, error = error) }
        }
    }

    fun onLoginClicked() {
        val state = _uiState.value
        if (state.isBusy) return

        val invalid = AuthValidator.validateLogin(state.email, state.password)
        if (invalid != null) {
            _uiState.update { it.copy(error = invalid) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the app switches to the signed-in screens by itself (SessionViewModel).
            val result = authRepository.login(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.asAuthError()) }
        }
    }
}
