package com.fintrack.kavindu.feature.expense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class HistoryRowKind { EXPENSE, UNRECORDED, INCOME }

/** One entry in the list. Wording that needs string resources ("Unrecorded spending", source names) is left to the screen. */
data class HistoryRow(
    val id: String,
    val kind: HistoryRowKind,
    val title: String?, // expense: the recurring item's name or the category's; null for UNRECORDED and INCOME
    val incomeSource: IncomeSource?, // INCOME only
    val details: String, // e.g. "Rent · Bank transfer · Main account"
    val amount: String, // e.g. "LKR 2,500"; an income starts with "+"
    val categoryType: CategoryType?, // expenses only, shown as a small label
    val isEstimate: Boolean, // a utility bill added by the app and not confirmed yet
    val canDelete: Boolean,
)

enum class DayKind { TODAY, YESTERDAY, OTHER }

data class HistoryDay(
    val key: String, // the ISO date, unique even across years (used as the list key)
    val kind: DayKind,
    val dateText: String, // "5 Nov", or "5 Nov 2025" for another year
    val rows: List<HistoryRow>,
)

/** The entry just deleted, so the screen can offer Undo. */
data class DeletedEntry(val expense: Expense)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val cycleLabel: String = "", // e.g. "25 Oct – 24 Nov"
    val isCurrentCycle: Boolean = true,
    val spent: String = "",
    val received: String = "",
    val days: List<HistoryDay> = emptyList(),
    val isQuickAddVisible: Boolean = false,
    val savedOutcome: SaveOutcome? = null, // set after quick-add saved; the screen shows its message once
    val deleted: DeletedEntry? = null, // set after a delete; the screen shows "Deleted · Undo" once
    val actionError: AppError? = null, // a confirm, delete or undo that could not be started
    // Search and filters (nothing saved)
    val query: String = "",
    val type: HistoryType = HistoryType.ALL,
    val isAllCycles: Boolean = false, // then the switcher shows "All cycles" and has no arrows
    val isFiltered: Boolean = false, // an empty list then means "no matches", not "nothing recorded"
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

/**
 * Transaction history (M8): everything recorded in one pay cycle, newest first, with edit, delete and Undo.
 * Past cycles are one tap away, so he can always look back and correct a mistake.
 * Reads from Room only, so it works offline and updates live when the Firestore listener brings in a change.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // 0 = this cycle, -1 = the one before, and so on.
    private val cycleOffset = MutableStateFlow(0)
    private val filter = MutableStateFlow(HistoryFilter())
    private var expensesById: Map<String, Expense> = emptyMap()
    private var loadJob: Job? = null

    init {
        load()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        val startDays = profileRepository.observeProfile()
            .map { (it?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY).coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY) }
            .distinctUntilChanged()

        val allCycles = filter.map { it.allCycles }.distinctUntilChanged()

        loadJob = viewModelScope.launch {
            combine(startDays, cycleOffset, allCycles) { startDay, offset, all -> Triple(cycleFor(startDay, offset), offset, all) }
                .flatMapLatest { (cycle, offset, all) ->
                    // "All cycles" reads every entry from Room; otherwise only the cycle on screen.
                    combine(
                        if (all) expenseRepository.observeExpensesSince(ALL_TIME) else expenseRepository.observeExpenses(cycle),
                        if (all) incomeRepository.observeIncomesSince(ALL_TIME) else incomeRepository.observeIncomes(cycle),
                        categoryRepository.observeCategories(),
                        recurringRepository.observeRecurringItems(),
                        filter,
                    ) { expenses, incomes, categories, items, currentFilter ->
                        expensesById = expenses.associateBy { it.id }
                        build(cycle, offset, expenses, incomes, categories, items, currentFilter)
                    }
                }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { built ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            cycleLabel = built.cycleLabel,
                            isCurrentCycle = built.isCurrentCycle,
                            spent = built.spent,
                            received = built.received,
                            days = built.days,
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    fun onPreviousCycleClicked() = cycleOffset.update { it - 1 }

    fun onNextCycleClicked() = cycleOffset.update { (it + 1).coerceAtMost(0) }

    // ---- search and filters ----

    fun onSearchChanged(text: String) = setFilter(filter.value.copy(query = text.take(MAX_QUERY_LENGTH)))

    fun onTypeSelected(type: HistoryType) = setFilter(filter.value.copy(type = type))

    fun onAllCyclesToggled() = setFilter(filter.value.copy(allCycles = !filter.value.allCycles))

    fun onClearFiltersClicked() = setFilter(HistoryFilter())

    // The screen shows the filter straight away; the list follows as soon as the new rows are built.
    private fun setFilter(newFilter: HistoryFilter) {
        filter.value = newFilter
        _uiState.update {
            it.copy(
                query = newFilter.query,
                type = newFilter.type,
                isAllCycles = newFilter.allCycles,
                isFiltered = newFilter.isActive,
            )
        }
    }

    // ---- quick add (the same sheet as on the dashboard) ----

    fun onAddExpenseClicked() = _uiState.update { it.copy(isQuickAddVisible = true) }

    fun onQuickAddDismissed() = _uiState.update { it.copy(isQuickAddVisible = false) }

    fun onExpenseSaved(outcome: SaveOutcome) = _uiState.update { it.copy(isQuickAddVisible = false, savedOutcome = outcome) }

    fun onSavedMessageShown() = _uiState.update { it.copy(savedOutcome = null) }

    // ---- delete with Undo, and confirming an estimate ----

    fun onDeleteClicked(id: String) {
        val expense = expensesById[id] ?: return
        viewModelScope.launch {
            val result = expenseRepository.deleteExpense(id)
            if (result.isSuccess) {
                _uiState.update { it.copy(deleted = DeletedEntry(expense)) }
            } else {
                _uiState.update { it.copy(actionError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    /** Undo saves the same expense again with the same id, which brings it back exactly as it was. */
    fun onUndoClicked() {
        val deleted = _uiState.value.deleted ?: return
        _uiState.update { it.copy(deleted = null) }
        viewModelScope.launch {
            val result = expenseRepository.saveExpense(deleted.expense)
            if (result.isFailure) _uiState.update { it.copy(actionError = result.exceptionOrNull()?.asAppError()) }
        }
    }

    fun onDeletedMessageShown() = _uiState.update { it.copy(deleted = null) }

    /** "Confirm" on a utility estimate: the amount is right as it is. */
    fun onConfirmEstimateClicked(id: String) {
        val expense = expensesById[id] ?: return
        viewModelScope.launch {
            val result = expenseRepository.saveExpense(expense.copy(isConfirmed = true))
            if (result.isFailure) _uiState.update { it.copy(actionError = result.exceptionOrNull()?.asAppError()) }
        }
    }

    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }

    // ---- building the list ----

    private class Built(
        val cycleLabel: String,
        val isCurrentCycle: Boolean,
        val spent: String,
        val received: String,
        val days: List<HistoryDay>,
    )

    private fun cycleFor(startDay: Int, offset: Int): PayCycle {
        val current = PayCycle.containing(LocalDate.now(), startDay)
        if (offset == 0) return current
        val start = current.start.plusMonths(offset.toLong())
        return PayCycle(start = start, end = start.plusMonths(1).minusDays(1))
    }

    private fun build(
        cycle: PayCycle,
        offset: Int,
        expenses: List<Expense>,
        incomes: List<Income>,
        categories: List<Category>,
        items: List<RecurringItem>,
        activeFilter: HistoryFilter,
    ): Built {
        val namesById = categories.associate { it.id to it.name }
        val today = LocalDate.now()

        // Filter first, so the list and the Spent / Received totals both show only what matches.
        val shownExpenses = expenses.filter { HistoryFilterRules.matches(it, namesById[it.categoryId], activeFilter) }
        val shownIncomes = incomes.filter { HistoryFilterRules.matches(it, activeFilter) }

        val expenseRows = shownExpenses.map { it.date to it.toRow(namesById, items) }
        val incomeRows = shownIncomes.map { it.date to it.toRow() }
        val days = (expenseRows + incomeRows)
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending<LocalDate> { it })
            .map { (date, rows) ->
                HistoryDay(
                    key = date.toString(),
                    kind = when (date) {
                        today -> DayKind.TODAY
                        today.minusDays(1) -> DayKind.YESTERDAY
                        else -> DayKind.OTHER
                    },
                    dateText = date.format(if (date.year == today.year) DAY_MONTH else DAY_MONTH_YEAR),
                    rows = rows,
                )
            }

        return Built(
            cycleLabel = if (activeFilter.allCycles) "" else "${cycle.start.format(DAY_MONTH)} – ${cycle.end.format(DAY_MONTH)}",
            isCurrentCycle = offset == 0,
            spent = Money.formatLkr(shownExpenses.sumOf { it.amountCents }),
            received = Money.formatLkr(shownIncomes.sumOf { it.lkrAmountCents }),
            days = days,
        )
    }

    private fun Expense.toRow(namesById: Map<String, String>, items: List<RecurringItem>): HistoryRow {
        val categoryName = namesById[categoryId]
        return when (origin) {
            ExpenseOrigin.BALANCE_CHECK -> HistoryRow(
                id = id, kind = HistoryRowKind.UNRECORDED, title = null, incomeSource = null,
                details = "", amount = Money.formatLkr(amountCents), categoryType = categoryType,
                isEstimate = false, canDelete = ExpenseRules.canDelete(this, items),
            )
            else -> {
                // An entry the app added shows the item's own name (e.g. "Netflix"); the category moves to the details.
                val title = if (origin == ExpenseOrigin.RECURRING) note ?: categoryName else categoryName
                val details = listOfNotNull(
                    categoryName.takeIf { origin == ExpenseOrigin.RECURRING && it != title },
                    channel,
                    account,
                    note.takeIf { origin != ExpenseOrigin.RECURRING },
                ).joinToString(" · ")
                HistoryRow(
                    id = id, kind = HistoryRowKind.EXPENSE, title = title, incomeSource = null,
                    details = details, amount = Money.formatLkr(amountCents), categoryType = categoryType,
                    isEstimate = ExpenseRules.isUnconfirmedEstimate(this),
                    canDelete = ExpenseRules.canDelete(this, items),
                )
            }
        }
    }

    private fun Income.toRow() = HistoryRow(
        id = id, kind = HistoryRowKind.INCOME, title = null, incomeSource = source,
        details = note.orEmpty(), amount = "+" + Money.formatLkr(lkrAmountCents), categoryType = null,
        isEstimate = false, canDelete = false, // an income is edited and deleted on its own screen
    )

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

        // "Every entry": Room compares dates as ISO text, and every entry in the app is dated after 2000.
        val ALL_TIME: LocalDate = LocalDate.of(2000, 1, 1)
        const val MAX_QUERY_LENGTH = 100
    }
}
