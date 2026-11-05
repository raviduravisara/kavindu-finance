package com.fintrack.kavindu.feature.recurring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Category
import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.feature.expense.viewmodel.CategoryOption
import com.fintrack.kavindu.feature.expense.viewmodel.ExpenseRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** A bill or subscription in the list. */
data class RecurringRow(
    val id: String,
    val name: String,
    val amount: String, // e.g. "LKR 34,000"
    val dueDay: Int, // day of the month, 1-28
    val categoryName: String?,
    val isEstimate: Boolean,
    val isSubscription: Boolean,
    val isActive: Boolean,
    val lastChecked: String?, // subscriptions only, e.g. "12 Oct"; null if never answered
)

/** The add / edit dialog. [id] is null while adding a new item. */
data class RecurringEditor(
    val id: String?,
    val nameInput: String = "",
    val amountInput: String = "",
    val dayInput: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<CategoryOption> = emptyList(),
    val isEstimate: Boolean = false,
    val isSubscription: Boolean = false,
    val isActive: Boolean = true,
    val nameError: RecurringFieldError? = null,
    val amountError: RecurringFieldError? = null,
    val dayError: RecurringFieldError? = null,
    val categoryError: RecurringFieldError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isDeleteDialogVisible: Boolean = false,
)

data class RecurringUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val subscriptions: List<RecurringRow> = emptyList(), // active subscriptions, each with "Still using this?"
    val monthlySubscriptions: String? = null, // e.g. "LKR 1,799"; null when there are none
    val yearlySubscriptions: String? = null,
    val bills: List<RecurringRow> = emptyList(), // active items that are not subscriptions (rent, gym, utilities)
    val paused: List<RecurringRow> = emptyList(),
    val editor: RecurringEditor? = null,
    val actionError: AppError? = null,
) {
    val isEmpty: Boolean get() = subscriptions.isEmpty() && bills.isEmpty() && paused.isEmpty()
}

/**
 * Recurring items and the subscription review (M5, S6): rent, gym and subscriptions are entered once
 * and added by the app every month, so silent spending never stays silent (RC1, RC2).
 *
 * For every subscription the screen asks "Still using this?". Answering yes restarts a 60-day clock; answering no
 * pauses the item so the app stops adding it. Kavindu meant to cancel two subscriptions six months ago and never
 * did: a calm, regular question with the real monthly cost beside it is how he finally decides.
 */
@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private var items: List<RecurringItem> = emptyList()
    private var categories: List<Category> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            combine(recurringRepository.observeRecurringItems(), categoryRepository.observeCategories()) { i, c -> i to c }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { (loadedItems, loadedCategories) ->
                    items = loadedItems
                    categories = loadedCategories
                    val namesById = loadedCategories.associate { it.id to it.name }
                    val rows = loadedItems.map { it.toRow(namesById) }
                    val monthly = RecurringRules.monthlySubscriptionCents(loadedItems)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            subscriptions = rows.filter { r -> r.isSubscription && r.isActive },
                            monthlySubscriptions = if (monthly > 0) Money.formatLkr(monthly) else null,
                            yearlySubscriptions = if (monthly > 0) Money.formatLkr(monthly * 12) else null,
                            bills = rows.filter { r -> !r.isSubscription && r.isActive },
                            paused = rows.filter { r -> !r.isActive },
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    // ---- subscription review ----

    /** "Still using it": the subscription stays, and he is not asked again for 60 days. */
    fun onStillUsingClicked(id: String) = saveItemChange(id) { it.copy(lastReviewedDate = LocalDate.now()) }

    /** "Not anymore": the app stops adding it. (He still has to cancel it with the provider.) */
    fun onPauseClicked(id: String) = saveItemChange(id) { it.copy(isActive = false, lastReviewedDate = LocalDate.now()) }

    fun onResumeClicked(id: String) = saveItemChange(id) { it.copy(isActive = true) }

    private fun saveItemChange(id: String, change: (RecurringItem) -> RecurringItem) {
        val item = items.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val result = recurringRepository.saveRecurringItem(change(item))
            if (result.isFailure) _uiState.update { it.copy(actionError = result.exceptionOrNull()?.asAppError()) }
        }
    }

    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }

    // ---- add / edit dialog ----

    fun onAddClicked() = _uiState.update { it.copy(editor = RecurringEditor(id = null, categories = categoryOptions())) }

    fun onItemClicked(id: String) {
        val item = items.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                editor = RecurringEditor(
                    id = item.id,
                    nameInput = item.name,
                    amountInput = ExpenseRules.toInputText(item.amountCents),
                    dayInput = item.dayOfMonth.toString(),
                    selectedCategoryId = item.categoryId.takeIf { c -> categories.any { it.id == c } },
                    categories = categoryOptions(),
                    isEstimate = item.isEstimate,
                    isSubscription = item.isSubscription,
                    isActive = item.isActive,
                )
            )
        }
    }

    fun onNameChange(text: String) = updateEditor { it.copy(nameInput = text, nameError = null, saveError = null) }

    fun onAmountChange(text: String) = updateEditor { it.copy(amountInput = text, amountError = null, saveError = null) }

    fun onDayChange(text: String) = updateEditor { it.copy(dayInput = text.filter { c -> c.isDigit() }.take(2), dayError = null) }

    fun onCategorySelected(id: String) = updateEditor { it.copy(selectedCategoryId = id, categoryError = null) }

    fun onEstimateChange(value: Boolean) = updateEditor { it.copy(isEstimate = value) }

    fun onSubscriptionChange(value: Boolean) = updateEditor { editor ->
        // Turning "subscription" on suggests the Subscriptions category, so he has one tap fewer to make.
        val suggested = categories.firstOrNull { it.name.equals(SUBSCRIPTIONS_CATEGORY, ignoreCase = true) }?.id
        editor.copy(
            isSubscription = value,
            selectedCategoryId = if (value && editor.selectedCategoryId == null) suggested else editor.selectedCategoryId,
        )
    }

    fun onActiveChange(value: Boolean) = updateEditor { it.copy(isActive = value) }

    fun onEditorDismissed() = _uiState.update { it.copy(editor = null) }

    fun onSaveClicked() {
        val editor = _uiState.value.editor ?: return
        if (editor.isSaving) return

        val check = RecurringRules.validate(editor.nameInput, editor.amountInput, editor.dayInput, editor.selectedCategoryId)
        updateEditor {
            it.copy(
                nameError = check.nameError,
                amountError = check.amountError,
                dayError = check.dayError,
                categoryError = check.categoryError,
            )
        }
        val cents = check.amountCents
        val day = check.dayOfMonth
        val categoryId = editor.selectedCategoryId
        if (!check.isValid || cents == null || day == null || categoryId == null) return

        updateEditor { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val existing = items.firstOrNull { it.id == editor.id }
            val item = RecurringItem(
                id = existing?.id ?: "", // blank: the repository creates the Firestore document
                name = check.name,
                amountCents = cents,
                categoryId = categoryId,
                dayOfMonth = day,
                isEstimate = editor.isEstimate,
                isSubscription = editor.isSubscription,
                isActive = editor.isActive,
                lastReviewedDate = existing?.lastReviewedDate,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val result = recurringRepository.saveRecurringItem(item)
            if (result.isSuccess) {
                _uiState.update { it.copy(editor = null) }
            } else {
                updateEditor { it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    // ---- delete ----

    fun onDeleteClicked() = updateEditor { if (it.id != null) it.copy(isDeleteDialogVisible = true) else it }

    fun onDeleteDismissed() = updateEditor { it.copy(isDeleteDialogVisible = false) }

    /** Removes the item. The entries it already added stay in history, so past totals do not change. */
    fun onDeleteConfirmed() {
        val id = _uiState.value.editor?.id ?: return
        updateEditor { it.copy(isDeleteDialogVisible = false, isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = recurringRepository.deleteRecurringItem(id)
            if (result.isSuccess) {
                _uiState.update { it.copy(editor = null) }
            } else {
                updateEditor { it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError()) }
            }
        }
    }

    private fun categoryOptions(): List<CategoryOption> = categories.map { CategoryOption(it.id, it.name) }

    private fun updateEditor(change: (RecurringEditor) -> RecurringEditor) =
        _uiState.update { state -> state.editor?.let { state.copy(editor = change(it)) } ?: state }

    private fun RecurringItem.toRow(namesById: Map<String, String>) = RecurringRow(
        id = id,
        name = name,
        amount = Money.formatLkr(amountCents),
        dueDay = dayOfMonth,
        categoryName = namesById[categoryId],
        isEstimate = isEstimate,
        isSubscription = isSubscription,
        isActive = isActive,
        lastChecked = lastReviewedDate?.format(DAY_MONTH),
    )

    private companion object {
        const val SUBSCRIPTIONS_CATEGORY = "Subscriptions"
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}
