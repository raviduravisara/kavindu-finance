package com.fintrack.kavindu.feature.expense.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.ExpenseOrigin
import com.fintrack.kavindu.core.model.StarterData
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.navigation.EditExpense
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

data class EditExpenseUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val amountInput: String = "",
    val categories: List<CategoryOption> = emptyList(),
    val selectedCategoryId: String? = null,
    val channel: String = "",
    val account: String = "",
    val channels: List<String> = StarterData.channels,
    val accounts: List<String> = StarterData.accounts,
    val noteInput: String = "",
    val dateText: String = "", // e.g. "25 Oct 2026"
    val dateMillis: Long = 0, // the selected date for the date picker (UTC midnight)
    val todayMillis: Long = 0, // the latest date he can pick
    val isDatePickerVisible: Boolean = false,
    // An entry the app added from a recurring item keeps its date: its id is built from that date.
    val isDateLocked: Boolean = false,
    val isEstimate: Boolean = false, // an unconfirmed utility estimate: saving confirms it
    val isUnrecorded: Boolean = false, // found by the weekly check-in
    val canDelete: Boolean = false,
    val amountError: ExpenseFieldError? = null,
    val categoryError: ExpenseFieldError? = null,
    val dateError: ExpenseFieldError? = null,
    val noteError: ExpenseFieldError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDone: Boolean = false, // saved or deleted: the screen closes
)

/**
 * Edit or delete one expense from history (M8). The screen only shows this state and sends events;
 * the form is checked by [ExpenseRules.validate] and saving goes through [ExpenseRepository].
 *
 * Kept from the original entry: its id, when it was created, how it was created and its recurring item, so an
 * entry the app added stays linked to its item and a recurring expense keeps the id the security rules require.
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
) : ViewModel() {

    private val expenseId: String = savedStateHandle.toRoute<EditExpense>().expenseId

    private val _uiState = MutableStateFlow(EditExpenseUiState())
    val uiState: StateFlow<EditExpenseUiState> = _uiState.asStateFlow()

    private var original: Expense? = null
    private var categories: List<Category> = emptyList()
    private var date: LocalDate = LocalDate.now()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                // Room holds every expense the listener has copied from Firestore.
                val expense = expenseRepository.observeExpensesSince(EARLIEST_DATE).first().firstOrNull { it.id == expenseId }
                if (expense == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = AppError.INVALID_DATA) }
                    return@launch
                }
                categories = categoryRepository.observeCategories().first()
                val items = recurringRepository.observeRecurringItems().first()
                original = expense
                setDate(expense.date)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        amountInput = ExpenseRules.toInputText(expense.amountCents),
                        categories = categories.map { c -> CategoryOption(c.id, c.name) },
                        selectedCategoryId = expense.categoryId.takeIf { id -> categories.any { c -> c.id == id } },
                        channel = expense.channel,
                        account = expense.account,
                        noteInput = expense.note.orEmpty(),
                        isDateLocked = expense.origin == ExpenseOrigin.RECURRING,
                        isEstimate = ExpenseRules.isUnconfirmedEstimate(expense),
                        isUnrecorded = expense.origin == ExpenseOrigin.BALANCE_CHECK,
                        canDelete = ExpenseRules.canDelete(expense, items),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = e.asAppError()) }
            }
        }
    }

    fun onRetryClicked() = load()

    // ---- form fields ----

    fun onAmountChange(text: String) = _uiState.update { it.copy(amountInput = text, amountError = null, saveError = null) }

    fun onCategorySelected(id: String) = _uiState.update { it.copy(selectedCategoryId = id, categoryError = null) }

    fun onChannelSelected(channel: String) = _uiState.update { it.copy(channel = channel) }

    fun onAccountSelected(account: String) = _uiState.update { it.copy(account = account) }

    fun onNoteChange(text: String) = _uiState.update { it.copy(noteInput = text, noteError = null) }

    fun onDateClicked() {
        if (_uiState.value.isDateLocked) return
        _uiState.update { it.copy(isDatePickerVisible = true, dateMillis = date.toUtcMillis()) }
    }

    fun onDatePickerDismissed() = _uiState.update { it.copy(isDatePickerVisible = false) }

    fun onDateSelected(utcMillis: Long?) {
        if (utcMillis != null) setDate(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())
        _uiState.update { it.copy(isDatePickerVisible = false, dateError = null) }
    }

    private fun setDate(newDate: LocalDate) {
        date = newDate
        _uiState.update {
            it.copy(
                dateText = newDate.format(DATE_FORMAT),
                dateMillis = newDate.toUtcMillis(),
                todayMillis = LocalDate.now().toUtcMillis(),
            )
        }
    }

    // ---- save and delete ----

    fun onSaveClicked() {
        val state = _uiState.value
        val current = original ?: return
        if (state.isSaving) return

        val check = ExpenseRules.validate(state.amountInput, state.selectedCategoryId, date, LocalDate.now(), state.noteInput)
        _uiState.update {
            it.copy(
                amountError = check.amountError,
                categoryError = check.categoryError,
                dateError = check.dateError,
                noteError = check.noteError,
            )
        }
        val category = categories.firstOrNull { it.id == state.selectedCategoryId }
        val cents = check.amountCents
        if (!check.isValid || category == null || cents == null) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val updated = current.copy(
                amountCents = cents,
                date = date,
                categoryId = category.id,
                categoryType = category.type,
                channel = state.channel,
                account = state.account,
                note = check.note,
                // Saving a utility estimate means he looked at it and the amount is right.
                isConfirmed = current.isConfirmed || ExpenseRules.isUnconfirmedEstimate(current),
            )
            val result = expenseRepository.saveExpense(updated)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isDone = true) }
            } else {
                _uiState.update { it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    fun onDeleteClicked() {
        if (_uiState.value.canDelete) _uiState.update { it.copy(isDeleteDialogVisible = true) }
    }

    fun onDeleteDismissed() = _uiState.update { it.copy(isDeleteDialogVisible = false) }

    fun onDeleteConfirmed() {
        _uiState.update { it.copy(isDeleteDialogVisible = false, isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = expenseRepository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isDone = true) }
            } else {
                _uiState.update { it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        val EARLIEST_DATE: LocalDate = LocalDate.of(2000, 1, 1)
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
