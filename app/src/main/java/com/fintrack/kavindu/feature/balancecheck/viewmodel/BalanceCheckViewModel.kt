package com.fintrack.kavindu.feature.balancecheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.BalanceCheckRepository
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.CryptoRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class GapKind { MATCHES, UNRECORDED_SPENDING, EXTRA_MONEY }

data class LastCheckInfo(val date: String, val total: String) // e.g. "14 Oct", "LKR 105,000.00"

data class CheckResult(
    val expected: String,
    val actual: String,
    val difference: String, // always shown as a positive amount
    val kind: GapKind,
)

data class BalanceCheckUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val lastCheck: LastCheckInfo? = null, // null: no check yet, so this one is the starting point
    val bankBalanceInput: String = "",
    val walletCashInput: String = "",
    val isInputInvalid: Boolean = false,
    val result: CheckResult? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isSaved: Boolean = false,
)

@HiltViewModel
class BalanceCheckViewModel @Inject constructor(
    private val balanceCheckRepository: BalanceCheckRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val cryptoRepository: CryptoRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalanceCheckUiState())
    val uiState: StateFlow<BalanceCheckUiState> = _uiState.asStateFlow()

    // The latest records the calculation needs, kept up to date from Room.
    private data class Records(
        val lastCheck: BalanceCheck?,
        val incomes: List<Income>,
        val expenses: List<Expense>,
        val transfers: List<CryptoTransfer>,
    )

    // A check worked out but not saved yet.
    private data class PendingCheck(val bankCents: Long, val walletCents: Long, val expectedCents: Long, val gapCents: Long)

    private var records: Records? = null
    private var categories: List<Category> = emptyList()
    private var pendingCheck: PendingCheck? = null
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            categoryRepository.observeCategories().catch { emit(emptyList()) }.collect { categories = it }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            balanceCheckRepository.observeLatestCheck()
                .flatMapLatest { last ->
                    if (last == null) {
                        flowOf(Records(null, emptyList(), emptyList(), emptyList()))
                    } else {
                        combine(
                            incomeRepository.observeIncomesSince(last.date),
                            expenseRepository.observeExpensesSince(last.date),
                            cryptoRepository.observeTransfers(),
                        ) { incomes, expenses, transfers -> Records(last, incomes, expenses, transfers) }
                    }
                }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { latest ->
                    records = latest
                    _uiState.update { it.copy(isLoading = false, lastCheck = latest.lastCheck?.toInfo()) }
                }
        }
    }

    fun onRetryClicked() = load()

    fun onBankBalanceChange(text: String) =
        _uiState.update { it.copy(bankBalanceInput = text, isInputInvalid = false, result = null, isSaved = false) }

    fun onWalletCashChange(text: String) =
        _uiState.update { it.copy(walletCashInput = text, isInputInvalid = false, result = null, isSaved = false) }

    // First check: there is nothing to compare with yet, so it only saves today's balances.
    fun onSaveStartingPointClicked() {
        val (bank, wallet) = readInputs() ?: return
        val check = BalanceCheck(
            id = "",
            date = LocalDate.now(),
            bankBalanceCents = bank,
            walletCashCents = wallet,
            isStartingPoint = true,
            createdAt = System.currentTimeMillis(),
        )
        save(check, unrecordedSpending = null, foundIncome = null)
    }

    fun onCheckClicked() {
        val (bank, wallet) = readInputs() ?: return
        val latest = records ?: return
        val lastCheck = latest.lastCheck ?: return

        val expected = BalanceCheckCalculator.expectedBalanceCents(lastCheck, latest.incomes, latest.expenses, latest.transfers)
        val actual = bank + wallet
        val gap = BalanceCheckCalculator.gapCents(expected, actual)
        pendingCheck = PendingCheck(bank, wallet, expected, gap)

        val kind = when {
            gap > 0 -> GapKind.UNRECORDED_SPENDING
            gap < 0 -> GapKind.EXTRA_MONEY
            else -> GapKind.MATCHES
        }
        _uiState.update {
            it.copy(
                result = CheckResult(
                    expected = Money.formatLkr(expected),
                    actual = Money.formatLkr(actual),
                    difference = Money.formatLkr(kotlin.math.abs(gap)),
                    kind = kind,
                ),
            )
        }
    }

    // Matches, or a positive gap: save the check (with one "unrecorded spending" entry if needed).
    fun onSaveCheckClicked() {
        val pending = pendingCheck ?: return
        val now = System.currentTimeMillis()
        val spending = if (pending.gapCents > 0) unrecordedSpending(pending.gapCents, now) else null
        save(pending.toCheck(now), unrecordedSpending = spending, foundIncome = null)
    }

    // Negative gap: the user chose to record the extra money as income.
    fun onAddGapAsIncomeClicked() {
        val pending = pendingCheck ?: return
        val now = System.currentTimeMillis()
        save(pending.toCheck(now), unrecordedSpending = null, foundIncome = foundIncome(-pending.gapCents, now))
    }

    // Negative gap: the user chose to ignore it; the check is saved as it is.
    fun onIgnoreGapClicked() {
        val pending = pendingCheck ?: return
        save(pending.toCheck(System.currentTimeMillis()), unrecordedSpending = null, foundIncome = null)
    }

    private fun readInputs(): Pair<Long, Long>? {
        val state = _uiState.value
        val bank = Money.parseToCents(state.bankBalanceInput)
        val wallet = Money.parseToCents(state.walletCashInput.ifBlank { "0" })
        if (bank == null || wallet == null) {
            _uiState.update { it.copy(isInputInvalid = true) }
            return null
        }
        return bank to wallet
    }

    private fun save(check: BalanceCheck, unrecordedSpending: Expense?, foundIncome: Income?) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = balanceCheckRepository.saveCheck(check, unrecordedSpending, foundIncome)
            pendingCheck = null
            _uiState.update {
                if (result.isSuccess) {
                    // Start fresh; the new check arrives from Room as the "last check".
                    BalanceCheckUiState(isLoading = false, lastCheck = it.lastCheck, isSaved = true)
                } else {
                    it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
                }
            }
        }
    }

    private fun PendingCheck.toCheck(now: Long) = BalanceCheck(
        id = "",
        date = LocalDate.now(),
        bankBalanceCents = bankCents,
        walletCashCents = walletCents,
        isStartingPoint = false,
        expectedCents = expectedCents,
        gapCents = gapCents,
        createdAt = now,
    )

    // Saved with the same time as the check, so the next check does not count it again.
    private fun unrecordedSpending(amountCents: Long, now: Long): Expense {
        val category = categories.firstOrNull { it.name == OTHER_CATEGORY }
            ?: categories.firstOrNull { it.type == CategoryType.DISCRETIONARY }
        return Expense(
            id = "",
            amountCents = amountCents,
            date = LocalDate.now(),
            categoryId = category?.id ?: FALLBACK_CATEGORY_ID,
            categoryType = category?.type ?: CategoryType.DISCRETIONARY,
            channel = UNRECORDED_CHANNEL,
            account = UNRECORDED_ACCOUNT,
            origin = ExpenseOrigin.BALANCE_CHECK,
            createdAt = now,
        )
    }

    private fun foundIncome(amountCents: Long, now: Long) = Income(
        id = "",
        source = IncomeSource.OTHER,
        amountCents = amountCents,
        currency = Currency.LKR,
        lkrAmountCents = amountCents,
        rateCentsPerUnit = 100, // LKR 1.00 per LKR
        date = LocalDate.now(),
        createdAt = now,
    )

    private fun BalanceCheck.toInfo() = LastCheckInfo(
        date = date.format(DAY_MONTH),
        total = Money.formatLkr(bankBalanceCents + walletCashCents),
    )

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        const val OTHER_CATEGORY = "Other"
        const val FALLBACK_CATEGORY_ID = "other"
        const val UNRECORDED_CHANNEL = "Cash"
        const val UNRECORDED_ACCOUNT = "Cash wallet"
    }
}
