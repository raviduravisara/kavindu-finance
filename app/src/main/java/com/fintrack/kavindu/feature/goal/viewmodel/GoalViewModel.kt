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
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.goal.domain.GoalWhatIfCalculator
import com.fintrack.kavindu.feature.goal.domain.WhatIfTiming
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
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

data class GoalUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val details: GoalDetails? = null, // null after loading = no goal set yet
    val whatIf: WhatIfState? = null, // null when the goal is reached or past its deadline
)

/** The "What if" card, already worked out and formatted. Nothing in it is saved. */
data class WhatIfState(
    val sliderValue: Float, // in steps of LKR 1,000
    val sliderMax: Float,
    val amount: String, // e.g. "LKR 45,000"
    val target: String, // e.g. "LKR 490,000"
    val pace: String?, // his average per completed cycle, e.g. "LKR 32,000"; null before the first full cycle
    val finishMonth: String?, // e.g. "July 2027"; null when the amount is 0
    val timing: WhatIfTiming?,
    val months: Int, // months before or after the deadline month
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

    // The amount he chose on the "What if" slider; null until he moves it (then it starts at his pace).
    private var whatIfAmountCents: Long? = null

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
                .collect { details ->
                    _uiState.value = GoalUiState(isLoading = false, details = details, whatIf = details?.let(::buildWhatIf))
                }
        }
    }

    fun onRetryClicked() = load()

    /** The slider moved; [value] counts steps of LKR 1,000. The finish month updates straight away. */
    fun onWhatIfAmountChanged(value: Float) {
        val details = _uiState.value.details ?: return
        whatIfAmountCents = value.roundToLong() * GoalWhatIfCalculator.STEP_CENTS
        _uiState.update { it.copy(whatIf = buildWhatIf(details)) }
    }

    private fun buildWhatIf(details: GoalDetails): WhatIfState? {
        val input = details.whatIfInput ?: return null
        val maxCents = GoalWhatIfCalculator.sliderMaxCents(input)
        val amountCents = (whatIfAmountCents ?: GoalWhatIfCalculator.startingAmountCents(input)).coerceIn(0, maxCents)
        val result = GoalWhatIfCalculator.finish(input, amountCents)
        return WhatIfState(
            sliderValue = (amountCents / GoalWhatIfCalculator.STEP_CENTS).toFloat(),
            sliderMax = (maxCents / GoalWhatIfCalculator.STEP_CENTS).toFloat(),
            amount = Money.formatLkr(amountCents),
            target = details.target,
            pace = if (input.averagePerCycleCents > 0) Money.formatLkr(input.averagePerCycleCents) else null,
            finishMonth = result?.finish?.format(MONTH_YEAR),
            timing = result?.timing,
            months = result?.months ?: 0,
        )
    }

    private companion object {
        val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    }
}
