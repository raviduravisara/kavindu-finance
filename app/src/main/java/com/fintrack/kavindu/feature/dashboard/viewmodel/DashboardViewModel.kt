package com.fintrack.kavindu.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.feature.dashboard.data.DashboardRepository
import com.fintrack.kavindu.feature.expense.viewmodel.SaveOutcome
import com.fintrack.kavindu.feature.recurring.data.RecurringEntryGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val details: DashboardDetails? = null,
    val isQuickAddVisible: Boolean = false,
    val savedOutcome: SaveOutcome? = null, // set after quick-add saved; the screen shows its message once
)

/**
 * The home screen (M7). One live stream: Room -> [DashboardRepository] -> [DashboardBuilder] -> this StateFlow ->
 * the screen. When the Firestore listener copies a change into Room, whether made on this phone or another one, the
 * numbers on screen move within moments. Nothing is calculated in the screen.
 *
 * It also starts [RecurringEntryGenerator] when the app opens, so rent, gym and subscriptions are already in the
 * totals by the time Kavindu sees them.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val recurringEntryGenerator: RecurringEntryGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
        addRecurringEntries()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            dashboardRepository.observeDashboard()
                .map { input -> DashboardBuilder.build(input) }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { details -> _uiState.update { it.copy(isLoading = false, loadError = null, details = details) } }
        }
    }

    private fun addRecurringEntries() {
        viewModelScope.launch {
            try {
                recurringEntryGenerator.run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Adding recurring entries is a background job: if it fails, the dashboard still works, and it
                // tries again the next time the app opens.
            }
        }
    }

    fun onRetryClicked() = load()

    fun onAddExpenseClicked() = _uiState.update { it.copy(isQuickAddVisible = true) }

    fun onQuickAddDismissed() = _uiState.update { it.copy(isQuickAddVisible = false) }

    fun onExpenseSaved(outcome: SaveOutcome) = _uiState.update { it.copy(isQuickAddVisible = false, savedOutcome = outcome) }

    fun onSavedMessageShown() = _uiState.update { it.copy(savedOutcome = null) }
}
