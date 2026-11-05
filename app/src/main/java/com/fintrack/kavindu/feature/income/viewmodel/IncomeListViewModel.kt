package com.fintrack.kavindu.feature.income.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CryptoRepository
import com.fintrack.kavindu.core.data.repository.FreelanceRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.crypto.viewmodel.CryptoCalculator
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceRules
import com.fintrack.kavindu.feature.freelance.viewmodel.observeProjectsWithMilestones
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

data class SourceTotal(val source: IncomeSource, val total: String)

data class IncomeRow(
    val id: String,
    val source: IncomeSource,
    val lkrAmount: String, // what reached the bank, e.g. "LKR 15,000"
    val original: String?, // only for USD/USDT, e.g. "USD 50"
    val rate: String?, // only for USD/USDT, e.g. "LKR 300" (per 1 unit)
    val currency: Currency,
    val date: String, // e.g. "25 Oct"
    val note: String?,
)

data class IncomeListUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val cycleLabel: String = "", // e.g. "25 Oct – 24 Nov"
    val totalReceived: String = "", // income entries + crypto profit that became real this cycle, as on the dashboard
    val cryptoProfit: String? = null, // e.g. "LKR 40,000"; null when no crypto profit became real this cycle
    val bySource: List<SourceTotal> = emptyList(), // only sources with income this cycle
    val rows: List<IncomeRow> = emptyList(),
    val freelancePending: String? = null, // e.g. "LKR 85,000": owed by clients, shown but not counted (FR-14)
    val overdueInvoices: Int = 0,
) {
    val isEmpty: Boolean get() = rows.isEmpty() && cryptoProfit == null
}

/**
 * This pay cycle's income, in total and per source (FR-10). Reads from Room only, so it works offline and
 * updates live when the Firestore listener brings in a change from another device.
 * Only received money is listed: a pending freelance invoice is not income until it is paid (FR-14).
 */
@HiltViewModel
class IncomeListViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val profileRepository: ProfileRepository,
    private val freelanceRepository: FreelanceRepository,
    private val cryptoRepository: CryptoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeListUiState())
    val uiState: StateFlow<IncomeListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            profileRepository.observeProfile()
                .map { profile ->
                    (profile?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY)
                        .coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)
                }
                .distinctUntilChanged()
                .flatMapLatest { startDay ->
                    val cycle = PayCycle.containing(LocalDate.now(), startDay)
                    combine(
                        incomeRepository.observeIncomes(cycle),
                        freelanceRepository.observeProjectsWithMilestones(),
                        cryptoRepository.observeTransfers(),
                    ) { incomes, projects, transfers -> buildState(cycle, incomes, projects.flatMap { it.second }, transfers) }
                }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onRetryClicked() = load()

    private fun buildState(
        cycle: PayCycle,
        incomes: List<Income>,
        milestones: List<Milestone>,
        transfers: List<CryptoTransfer>,
    ): IncomeListUiState {
        val bySource = IncomeSource.entries.mapNotNull { source ->
            val total = incomes.filter { it.source == source }.sumOf { it.lkrAmountCents }
            if (total > 0) SourceTotal(source, Money.formatLkr(total)) else null
        }
        // Crypto profit is calculated from the transfers, never saved as an income entry, so it is never counted twice.
        // It is added here exactly as the dashboard's "Received" adds it, so both screens show the same total.
        val cryptoProfit = CryptoCalculator.realisedBetween(CryptoCalculator.position(transfers), cycle.start, cycle.end)
        return IncomeListUiState(
            isLoading = false,
            cycleLabel = "${cycle.start.format(DAY_MONTH)} – ${cycle.end.format(DAY_MONTH)}",
            totalReceived = Money.formatLkr(incomes.sumOf { it.lkrAmountCents } + cryptoProfit),
            cryptoProfit = if (cryptoProfit > 0) Money.formatLkr(cryptoProfit) else null,
            bySource = bySource,
            rows = incomes.map { it.toRow() },
            freelancePending = FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(milestones)),
            overdueInvoices = milestones.count { FreelanceRules.isOverdue(it, LocalDate.now()) },
        )
    }

    private fun Income.toRow(): IncomeRow {
        val foreign = currency != Currency.LKR
        return IncomeRow(
            id = id,
            source = source,
            lkrAmount = Money.formatLkr(lkrAmountCents),
            original = if (foreign) IncomeFormat.amount(amountCents, currency) else null,
            rate = if (foreign) Money.formatLkr(rateCentsPerUnit) else null,
            currency = currency,
            date = date.format(DAY_MONTH),
            note = note,
        )
    }

    private companion object {
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}
