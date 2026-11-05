package com.fintrack.kavindu.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.reminder.data.ReminderScheduler
import com.fintrack.kavindu.feature.reminder.data.ReminderSettingsRepository
import com.fintrack.kavindu.feature.reminder.viewmodel.ReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    // Daily reminder (saved on this phone only)
    val isReminderEnabled: Boolean = false,
    val reminderTime: LocalTime = ReminderTime.DEFAULT, // for the time picker
    val reminderTimeText: String = "", // e.g. "8:00 PM"
    val isReminderTimeDialogVisible: Boolean = false,
    val isReminderPermissionNoteVisible: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderSettingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        isReminderEnabled = settings.isEnabled,
                        reminderTime = settings.time,
                        reminderTimeText = settings.time.format(SHORT_TIME),
                    )
                }
            }
        }
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

    /** The switch was turned on or off. On Android 13+ the screen asks for the notification permission first. */
    fun onReminderToggled(enabled: Boolean) {
        reminderScheduler.update(reminderSettingsRepository.current().copy(isEnabled = enabled))
        _uiState.update { it.copy(isReminderPermissionNoteVisible = false) }
    }

    /** The answer to Android's "Allow notifications?" question. Without it, the reminder stays off. */
    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            onReminderToggled(true)
        } else {
            _uiState.update { it.copy(isReminderPermissionNoteVisible = true) }
        }
    }

    fun onReminderTimeClicked() = _uiState.update { it.copy(isReminderTimeDialogVisible = true) }

    fun onReminderTimeDismissed() = _uiState.update { it.copy(isReminderTimeDialogVisible = false) }

    fun onReminderTimeSelected(hour: Int, minute: Int) {
        _uiState.update { it.copy(isReminderTimeDialogVisible = false) }
        reminderScheduler.update(reminderSettingsRepository.current().copy(time = LocalTime.of(hour, minute)))
    }

    fun onLogoutClicked() = _uiState.update { it.copy(isLogoutDialogVisible = true) }

    fun onLogoutDismissed() = _uiState.update { it.copy(isLogoutDialogVisible = false) }

    fun onLogoutConfirmed() {
        _uiState.update { it.copy(isLogoutDialogVisible = false) }
        // The reminder belongs to this user: the next person who signs in on this phone does not get it.
        reminderScheduler.turnOff()
        // Logout clears Room; the app then switches to the login screen by itself (SessionViewModel).
        viewModelScope.launch { authRepository.logout() }
    }

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val SHORT_TIME: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
}
