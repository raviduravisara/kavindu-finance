package com.fintrack.kavindu.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.feature.settings.data.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppThemeUiState(val mode: ThemeMode = ThemeMode.SYSTEM)

/**
 * Gives MainActivity the Appearance choice, so the whole app switches between light and dark straight away.
 * It starts with the saved value, so the first frame is already drawn in the right theme.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    themeRepository: ThemeRepository,
) : ViewModel() {

    val uiState: StateFlow<AppThemeUiState> = themeRepository.mode
        .map { AppThemeUiState(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeUiState(themeRepository.mode.value))
}
