package com.fintrack.kavindu.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.util.PayCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class SettingsMessage { SAVED, SAVE_FAILED }

data class SettingsUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val cycleStartDay: Int = PayCycle.DEFAULT_START_DAY,
    val cycleStart: String = "", // e.g. "25 Sep"
    val cycleEnd: String = "", // e.g. "24 Oct"
    val message: SettingsMessage? = null,
    val isLogoutDialogVisible: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                if (profile == null) return@collect // still arriving from Firestore: keep showing loading

                val startDay = profile.cycleStartDay.coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)
                val cycle = PayCycle.containing(LocalDate.now(), startDay)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = profile.email,
                        cycleStartDay = startDay,
                        cycleStart = cycle.start.format(DAY_MONTH),
                        cycleEnd = cycle.end.format(DAY_MONTH),
                    )
                }
            }
        }
    }

    fun onStartDaySelected(day: Int) {
        if (day == _uiState.value.cycleStartDay) return
        viewModelScope.launch {
            // The new day comes back through Room, which updates the screen and the cycle dates.
            val result = profileRepository.updateCycleStartDay(day)
            _uiState.update {
                it.copy(message = if (result.isSuccess) SettingsMessage.SAVED else SettingsMessage.SAVE_FAILED)
            }
        }
    }

    fun onLogoutClicked() = _uiState.update { it.copy(isLogoutDialogVisible = true) }

    fun onLogoutDismissed() = _uiState.update { it.copy(isLogoutDialogVisible = false) }

    fun onLogoutConfirmed() {
        _uiState.update { it.copy(isLogoutDialogVisible = false) }
        // Logout clears Room; the app then switches to the login screen by itself (SessionViewModel).
        viewModelScope.launch { authRepository.logout() }
    }

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}
