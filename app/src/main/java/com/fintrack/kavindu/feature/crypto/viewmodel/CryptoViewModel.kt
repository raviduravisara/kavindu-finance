package com.fintrack.kavindu.feature.crypto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CryptoRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransferRow(
    val id: String,
    val direction: CryptoDirection,
    val amount: String, // e.g. "LKR 25,000"
    val date: String, // e.g. "24 Oct"
    val note: String?,
)

data class CryptoUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val net: String = "LKR 0",
    val isNetNegative: Boolean = false,
    val totalIn: String = "LKR 0",
    val totalOut: String = "LKR 0",
    val realisedThisCycle: String = "LKR 0",
    val realisedTotal: String = "LKR 0",
    val rows: List<TransferRow> = emptyList(),
    // Add transfer dialog
    val isAddDialogVisible: Boolean = false,
    val direction: CryptoDirection = CryptoDirection.INTO_BINANCE,
    val amountInput: String = "",
    val noteInput: String = "",
    val dateText: String = "",
    val isAmountInvalid: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val dateMillis: Long = 0,
    val todayMillis: Long = 0,
    // Delete dialog
    val deletingRow: TransferRow? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
)

/**
 * The crypto card (S2): money moved into and out of Binance in LKR, the net position, and the profit that
 * became real. All maths is in [CryptoCalculator]; this ViewModel only reads Room and formats.
 */
@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CryptoUiState())
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    private var transfers: List<CryptoTransfer> = emptyList()
    private var date: LocalDate = LocalDate.now()
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            combine(cryptoRepository.observeTransfers(), profileRepository.observeProfile()) { items, profile ->
                items to (profile?.cycleStartDay ?: PayCycle.DEFAULT_START_DAY).coerceIn(PayCycle.MIN_START_DAY, PayCycle.MAX_START_DAY)
            }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { (items, startDay) ->
                    transfers = items
                    val today = LocalDate.now()
                    val cycle = PayCycle.containing(today, startDay)
                    val position = CryptoCalculator.position(items)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            net = Money.formatLkr(position.netCents),
                            isNetNegative = position.netCents < 0,
                            totalIn = Money.formatLkr(position.totalInCents),
                            totalOut = Money.formatLkr(position.totalOutCents),
                            realisedThisCycle = Money.formatLkr(CryptoCalculator.realisedBetween(position, cycle.start, cycle.end)),
                            realisedTotal = Money.formatLkr(position.totalRealisedCents),
                            rows = items
                                .sortedWith(compareByDescending<CryptoTransfer> { t -> t.date }.thenByDescending { t -> t.createdAt })
                                .map { t -> t.toRow() },
                            todayMillis = today.toUtcMillis(),
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    fun onAddTransferClicked() {
        date = LocalDate.now()
        _uiState.update {
            it.copy(
                isAddDialogVisible = true, direction = CryptoDirection.INTO_BINANCE, amountInput = "", noteInput = "",
                dateText = date.format(DATE_FORMAT), isAmountInvalid = false, saveError = null,
            )
        }
    }

    fun onAddTransferDismissed() = _uiState.update { it.copy(isAddDialogVisible = false) }

    fun onDirectionSelected(direction: CryptoDirection) = _uiState.update { it.copy(direction = direction) }

    fun onAmountChange(text: String) = _uiState.update { it.copy(amountInput = text, isAmountInvalid = false) }

    fun onNoteChange(text: String) = _uiState.update { it.copy(noteInput = text.take(MAX_NOTE_LENGTH)) }

    fun onDateClicked() = _uiState.update { it.copy(isDatePickerVisible = true, dateMillis = date.toUtcMillis()) }

    fun onDatePickerDismissed() = _uiState.update { it.copy(isDatePickerVisible = false) }

    fun onDateSelected(utcMillis: Long?) {
        if (utcMillis != null) date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        _uiState.update { it.copy(isDatePickerVisible = false, dateText = date.format(DATE_FORMAT)) }
    }

    fun onSaveTransferClicked() {
        val state = _uiState.value
        if (state.isSaving) return
        val amount = CryptoCalculator.parseAmount(state.amountInput)
        if (amount == null) {
            _uiState.update { it.copy(isAmountInvalid = true) }
            return
        }
        val transfer = CryptoTransfer(
            id = "", // blank id = new document
            direction = state.direction,
            amountCents = amount,
            date = minOf(date, LocalDate.now()), // a transfer cannot be in the future
            note = state.noteInput.trim().ifEmpty { null },
            createdAt = System.currentTimeMillis(),
        )
        save { cryptoRepository.saveTransfer(transfer) }
    }

    fun onDeleteClicked(id: String) {
        val row = transfers.firstOrNull { it.id == id }?.toRow() ?: return
        _uiState.update { it.copy(deletingRow = row, saveError = null) }
    }

    fun onDeleteDismissed() = _uiState.update { it.copy(deletingRow = null) }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deletingRow?.id ?: return
        save { cryptoRepository.deleteTransfer(id) }
    }

    private fun save(action: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = action()
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isAddDialogVisible = false, deletingRow = null)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    private fun CryptoTransfer.toRow() = TransferRow(
        id = id,
        direction = direction,
        amount = Money.formatLkr(amountCents),
        date = date.format(DAY_MONTH),
        note = note,
    )

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        const val MAX_NOTE_LENGTH = 200
    }
}
