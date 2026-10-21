package com.fintrack.kavindu

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.ErrorRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.ui.components.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppMessagesUiState(val error: AppError? = null)

// App-wide messages for problems that happen in the background, on any screen.
@HiltViewModel
class AppMessagesViewModel @Inject constructor(
    errorRepository: ErrorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppMessagesUiState())
    val uiState: StateFlow<AppMessagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            errorRepository.backgroundErrors.collect { error -> _uiState.update { it.copy(error = error) } }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(error = null) }
}

// Shows each background error once, as a snackbar at the bottom of the screen.
@Composable
fun AppMessages(
    snackbarHostState: SnackbarHostState,
    viewModel: AppMessagesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val error = uiState.error ?: return
    val message = stringResource(error.messageRes())
    LaunchedEffect(error) {
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }
}
