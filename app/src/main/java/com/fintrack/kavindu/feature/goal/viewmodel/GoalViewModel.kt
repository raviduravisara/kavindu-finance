package com.fintrack.kavindu.feature.goal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.GoalRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.PayCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val details: GoalDetails? = null, // null after loading = no goal set yet
)

/**
 * The Goal tab (M6). Reads the goal, the pay cycle, and all income and expenses since the goal started
 * from Room, and hands them to [GoalOverviewBuilder]. It updates live: a new expense on any device moves
 * the numbers and the "What changed your date" list straight away.
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        val startDays = profileRepository.observeProfile()
            .map { profile ->
                (profile?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY).coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)
            }
            .distinctUntilChanged()

        loadJob = viewModelScope.launch {
            combine(goalRepository.observeGoal(), startDays) { goal, startDay -> goal to startDay }
                .flatMapLatest { (goal, startDay) ->
                    if (goal == null) {
                        flowOf<GoalDetails?>(null)
                    } else {
                        combine(
                            incomeRepository.observeIncomesSince(goal.startDate),
                            expenseRepository.observeExpensesSince(goal.startDate),
                            categoryRepository.observeCategories(),
                        ) { incomes, expenses, categories ->
                            val today = LocalDate.now()
                            GoalOverviewBuilder.build(goal, incomes, expenses, categories, PayCycle.containing(today, startDay), today)
                        }
                    }
                }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { details -> _uiState.value = GoalUiState(isLoading = false, details = details) }
        }
    }

    fun onRetryClicked() = load()
}
