package com.fintrack.kavindu.feature.income.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.navigation.EditIncome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditIncomeUiState(
    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isEditing: Boolean = false, // false = new income
    val source: IncomeSource = IncomeSource.SALARY,
    val currency: Currency = Currency.LKR,
    val amountInput: String = "",
    val lkrReceivedInput: String = "",
    val noteInput: String = "",
    val dateText: String = "", // e.g. "25 Oct 2026"
    val dateMillis: Long = 0, // the selected date for the date picker (UTC midnight)
    val maxDateMillis: Long = 0, // today: income is recorded when it arrives
    val isDatePickerVisible: Boolean = false,
    val ratePreview: String? = null, // e.g. "LKR 300" (per 1 USD), shown once both amounts are valid
    val amountError: IncomeFieldError? = null,
    val lkrReceivedError: IncomeFieldError? = null,
    val dateError: IncomeFieldError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDone: Boolean = false, // saved or deleted: the screen closes
    // Created by "Mark received" on a freelance milestone: its source, currency and delete stay locked,
    // so the milestone never points to an income that was removed or changed into another source.
    val isLinkedToProject: Boolean = false,
) {
    val showsLkrReceived: Boolean get() = currency != Currency.LKR
}

/**
 * Add a new income or edit an existing one (M2). The screen only shows this state and sends events;
 * checking the form is done by [IncomeInputValidator], and saving goes through [IncomeRepository]
 * (Firestore first, then the listener copies it into Room).
 */
@HiltViewModel
class EditIncomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val incomeId: String? = savedStateHandle.toRoute<EditIncome>().incomeId

    private val _uiState = MutableStateFlow(EditIncomeUiState())
    val uiState: StateFlow<EditIncomeUiState> = _uiState.asStateFlow()

    private var date: LocalDate = LocalDate.now()

    // The income being edited. Its id, createdAt and freelance links are kept when saving.
    private var original: Income? = null

    init {
        setDate(LocalDate.now())
        if (incomeId != null) load(incomeId)
    }

    private fun load(id: String) {
        _uiState.update { it.copy(isLoading = true, loadError = null, isEditing = true) }
        viewModelScope.launch {
            try {
                // Room holds every income the listener has copied from Firestore.
                val income = incomeRepository.observeIncomesSince(EARLIEST_DATE).first().firstOrNull { it.id == id }
                if (income == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = AppError.INVALID_DATA) }
                    return@launch
                }
                original = income
                setDate(income.date)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        source = income.source,
                        currency = income.currency,
                        amountInput = IncomeFormat.toInputText(income.amountCents),
                        lkrReceivedInput = if (income.currency == Currency.LKR) "" else IncomeFormat.toInputText(income.lkrAmountCents),
                        noteInput = income.note.orEmpty(),
                        isLinkedToProject = income.milestoneId != null,
                    )
                }
                refreshRatePreview()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = e.asAppError()) }
            }
        }
    }

    fun onRetryClicked() {
        incomeId?.let(::load)
    }

    fun onSourceSelected(source: IncomeSource) {
        if (_uiState.value.isLinkedToProject) return
        _uiState.update { it.copy(source = source) }
    }

    fun onCurrencySelected(currency: Currency) {
        if (_uiState.value.isLinkedToProject) return
        _uiState.update { it.copy(currency = currency, lkrReceivedError = null) }
        refreshRatePreview()
    }

    fun onAmountChange(text: String) {
        _uiState.update { it.copy(amountInput = text, amountError = null) }
        refreshRatePreview()
    }

    fun onLkrReceivedChange(text: String) {
        _uiState.update { it.copy(lkrReceivedInput = text, lkrReceivedError = null) }
        refreshRatePreview()
    }

    fun onNoteChange(text: String) = _uiState.update { it.copy(noteInput = text.take(MAX_NOTE_LENGTH)) }

    fun onDateClicked() = _uiState.update { it.copy(isDatePickerVisible = true) }

    fun onDatePickerDismissed() = _uiState.update { it.copy(isDatePickerVisible = false) }

    /** The date picker gives the chosen day as UTC midnight in milliseconds. */
    fun onDateSelected(utcMillis: Long?) {
        if (utcMillis != null) {
            setDate(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())
        }
        _uiState.update { it.copy(isDatePickerVisible = false, dateError = null) }
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (state.isSaving) return
        val today = LocalDate.now()
        val check = IncomeInputValidator.validate(
            IncomeFormInput(state.currency, state.amountInput, state.lkrReceivedInput, date),
            today,
        )
        if (!check.isValid) {
            _uiState.update {
                it.copy(amountError = check.amountError, lkrReceivedError = check.lkrReceivedError, dateError = check.dateError)
            }
            return
        }
        val old = original
        val income = Income(
            id = old?.id ?: "", // blank id = new document
            source = state.source,
            amountCents = check.amountCents!!,
            currency = state.currency,
            lkrAmountCents = check.lkrAmountCents!!,
            rateCentsPerUnit = check.rateCentsPerUnit!!,
            date = date,
            note = state.noteInput.trim().ifEmpty { null },
            projectId = old?.projectId,
            milestoneId = old?.milestoneId,
            createdAt = old?.createdAt ?: System.currentTimeMillis(),
        )
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = incomeRepository.saveIncome(income)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isDone = true)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    fun onDeleteClicked() {
        if (_uiState.value.isLinkedToProject) return
        _uiState.update { it.copy(isDeleteDialogVisible = true) }
    }

    fun onDeleteDismissed() = _uiState.update { it.copy(isDeleteDialogVisible = false) }

    fun onDeleteConfirmed() {
        val id = original?.id ?: return
        _uiState.update { it.copy(isDeleteDialogVisible = false, isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = incomeRepository.deleteIncome(id)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isDone = true)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    private fun setDate(newDate: LocalDate) {
        date = newDate
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                dateText = newDate.format(DATE_FORMAT),
                dateMillis = newDate.toUtcMillis(),
                maxDateMillis = today.toUtcMillis(),
            )
        }
    }

    // Shows "1 USD = LKR 300" as soon as both amounts are valid, so a typo is easy to spot.
    private fun refreshRatePreview() {
        val state = _uiState.value
        val amount = Money.parseToCents(state.amountInput)?.takeIf { it > 0 }
        val lkr = Money.parseToCents(state.lkrReceivedInput)?.takeIf { it > 0 }
        val preview = if (state.currency != Currency.LKR && amount != null && lkr != null) {
            Money.formatLkr(IncomeInputValidator.rateCentsPerUnit(amount, lkr))
        } else {
            null
        }
        _uiState.update { it.copy(ratePreview = preview) }
    }

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        val EARLIEST_DATE: LocalDate = LocalDate.of(2000, 1, 1)
        const val MAX_NOTE_LENGTH = 200
    }
}
