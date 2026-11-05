package com.fintrack.kavindu.feature.expense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.GoalRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.StarterData
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.dashboard.viewmodel.GoalSnapshots
import com.fintrack.kavindu.feature.goal.domain.GoalImpactCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** One category chip. */
data class CategoryOption(val id: String, val name: String)

/**
 * What happened when an expense was saved, so the screen can show the calm message under it (FR-32).
 * [daysDelayed] is only meaningful for a discretionary expense when [goalName] is not null.
 */
data class SaveOutcome(val isDiscretionary: Boolean, val goalName: String?, val daysDelayed: Int)

data class QuickAddUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val amountInput: String = "", // what has been typed on the keypad, e.g. "2500"
    val amountDisplay: String = "0", // the same, grouped for the big number, e.g. "2,500"
    val categories: List<CategoryOption> = emptyList(), // most-used first
    val selectedCategoryId: String? = null,
    val channel: String = StarterData.channels.first(),
    val account: String = StarterData.accounts.first(),
    val channels: List<String> = StarterData.channels,
    val accounts: List<String> = StarterData.accounts,
    val dateIsToday: Boolean = true,
    val dateText: String = "", // e.g. "12 Nov"; the screen shows "Today" when [dateIsToday]
    val dateMillis: Long = 0, // the selected date for the date picker (UTC midnight)
    val todayMillis: Long = 0, // the latest date he can pick: an expense is recorded on or after the day it happened
    val isDatePickerVisible: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val outcome: SaveOutcome? = null, // set once saved; the screen closes the sheet and shows the message
) {
    val canSave: Boolean get() = !isSaving && ExpenseRules.canSave(AmountKeypad.cents(amountInput), selectedCategoryId)
}

/**
 * The quick-add sheet (M3): amount, category, Save. It opens with the keypad ready, remembers the last channel and
 * account, and lists the categories he uses most first, so recording an expense is 3 taps (FR-18, FR-19, RC2).
 *
 * The screen only shows this state and sends events. Typing rules are in [AmountKeypad], the other rules in
 * [ExpenseRules]; saving goes through [ExpenseRepository] (Firestore first, then the listener copies it into Room),
 * so it also works offline and the dashboard updates the moment it is saved.
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    private var categories: List<Category> = emptyList()
    private var date: LocalDate = LocalDate.now()

    // What he used last time. They fill the "Paid with" chips until he picks something else.
    private var rememberedChannel: String? = null
    private var rememberedAccount: String? = null
    private var channelPicked = false
    private var accountPicked = false

    private var loadJob: Job? = null

    init {
        load()
        setDate(LocalDate.now())
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            combine(
                categoryRepository.observeCategories(),
                expenseRepository.observeExpensesSince(LocalDate.now().minusDays(USAGE_DAYS)),
                profileRepository.observeProfile(),
            ) { allCategories, recent, profile -> Triple(allCategories, recent, profile) }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { (allCategories, recent, profile) ->
                    categories = allCategories
                    rememberedChannel = profile?.lastChannel?.takeIf { it in StarterData.channels }
                    rememberedAccount = profile?.lastAccount?.takeIf { it in StarterData.accounts }
                    val ordered = ExpenseRules.orderByUsage(allCategories, recent)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = null,
                            categories = ordered.map { CategoryOption(it.id, it.name) },
                            selectedCategoryId = state.selectedCategoryId?.takeIf { id -> ordered.any { it.id == id } },
                            channel = if (channelPicked) state.channel else rememberedChannel ?: state.channel,
                            account = if (accountPicked) state.account else rememberedAccount ?: state.account,
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    /** The sheet was opened: start from a clean amount, today's date and last time's channel and account. */
    fun onSheetOpened() {
        channelPicked = false
        accountPicked = false
        setDate(LocalDate.now())
        _uiState.update {
            it.copy(
                amountInput = "",
                amountDisplay = AmountKeypad.display(""),
                selectedCategoryId = null,
                channel = rememberedChannel ?: StarterData.channels.first(),
                account = rememberedAccount ?: StarterData.accounts.first(),
                isSaving = false,
                saveError = null,
                outcome = null,
                isDatePickerVisible = false,
            )
        }
    }

    /** The screen has taken the outcome (to close the sheet and show its message), so it is not delivered twice. */
    fun onOutcomeConsumed() = _uiState.update { it.copy(outcome = null) }

    // ---- keypad ----

    fun onDigitPressed(digit: Char) = changeAmount(AmountKeypad.appendDigit(_uiState.value.amountInput, digit))

    fun onDotPressed() = changeAmount(AmountKeypad.appendDot(_uiState.value.amountInput))

    fun onBackspacePressed() = changeAmount(AmountKeypad.backspace(_uiState.value.amountInput))

    private fun changeAmount(text: String) =
        _uiState.update { it.copy(amountInput = text, amountDisplay = AmountKeypad.display(text), saveError = null) }

    // ---- chips ----

    fun onCategorySelected(id: String) = _uiState.update { it.copy(selectedCategoryId = id, saveError = null) }

    fun onChannelSelected(channel: String) {
        channelPicked = true
        _uiState.update { it.copy(channel = channel) }
    }

    fun onAccountSelected(account: String) {
        accountPicked = true
        _uiState.update { it.copy(account = account) }
    }

    // ---- date ----

    fun onDateChipClicked() = _uiState.update { it.copy(isDatePickerVisible = true, dateMillis = date.toUtcMillis()) }

    fun onDatePickerDismissed() = _uiState.update { it.copy(isDatePickerVisible = false) }

    fun onDateSelected(utcMillis: Long?) {
        if (utcMillis != null) {
            val picked = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
            if (!picked.isAfter(LocalDate.now())) setDate(picked)
        }
        _uiState.update { it.copy(isDatePickerVisible = false) }
    }

    private fun setDate(newDate: LocalDate) {
        date = newDate
        _uiState.update {
            it.copy(
                dateIsToday = newDate == LocalDate.now(),
                dateText = newDate.format(DAY_MONTH),
                dateMillis = newDate.toUtcMillis(),
                todayMillis = LocalDate.now().toUtcMillis(),
            )
        }
    }

    // ---- save ----

    fun onSaveClicked() {
        val state = _uiState.value
        val cents = AmountKeypad.cents(state.amountInput) ?: return
        val category = categories.firstOrNull { it.id == state.selectedCategoryId } ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val expense = Expense(
                id = "", // blank: the repository creates the Firestore document
                amountCents = cents,
                currency = Currency.LKR,
                date = date,
                categoryId = category.id,
                categoryType = category.type, // copied now, so old cycles keep their totals if the flag changes later
                channel = state.channel,
                account = state.account,
                note = null,
                origin = ExpenseOrigin.MANUAL,
                createdAt = System.currentTimeMillis(),
            )
            val saved = expenseRepository.saveExpense(expense)
            if (saved.isFailure) {
                _uiState.update { it.copy(isSaving = false, saveError = saved.exceptionOrNull()?.asAppError()) }
                return@launch
            }
            // Remember this channel and account for next time. Failing to remember is not worth bothering him with.
            if (state.channel != rememberedChannel || state.account != rememberedAccount) {
                profileRepository.updateLastChannelAndAccount(state.channel, state.account)
            }
            val outcome = outcomeFor(category.type, cents)
            _uiState.update { it.copy(isSaving = false, outcome = outcome) }
        }
    }

    /**
     * After a discretionary expense: how many days it moves the goal date (S5). The goal numbers do not depend
     * on the expense just saved, so reading them now gives the same answer as reading them after.
     * Any problem here only means no message, never a failed save.
     */
    private suspend fun outcomeFor(type: CategoryType, cents: Long): SaveOutcome {
        if (type != CategoryType.DISCRETIONARY) return SaveOutcome(isDiscretionary = false, goalName = null, daysDelayed = 0)
        return try {
            val goal = goalRepository.observeGoal().first()
                ?: return SaveOutcome(isDiscretionary = true, goalName = null, daysDelayed = 0)
            val startDay = (profileRepository.observeProfile().first()?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY)
                .coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)
            val today = LocalDate.now()
            val cycle = PayCycle.containing(today, startDay)
            val since = minOf(goal.startDate, cycle.start)
            val progress = GoalSnapshots.progress(
                goal = goal,
                incomes = incomeRepository.observeIncomesSince(since).first(),
                expenses = expenseRepository.observeExpensesSince(since).first(),
                cycle = cycle,
                today = today,
            )
            val days = GoalImpactCalculator().daysDelayed(cents, progress.neededThisCycleCents, today, startDay)
            SaveOutcome(isDiscretionary = true, goalName = goal.name, daysDelayed = days)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveOutcome(isDiscretionary = true, goalName = null, daysDelayed = 0)
        }
    }

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val USAGE_DAYS = 90L // "most-used" looks at the last three months
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}
