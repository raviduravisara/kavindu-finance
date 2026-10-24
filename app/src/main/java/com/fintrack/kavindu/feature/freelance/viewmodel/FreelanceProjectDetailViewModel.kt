package com.fintrack.kavindu.feature.freelance.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fintrack.kavindu.core.data.repository.FreelanceRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.FreelanceProject
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.feature.income.viewmodel.IncomeFieldError
import com.fintrack.kavindu.feature.income.viewmodel.IncomeFormInput
import com.fintrack.kavindu.feature.income.viewmodel.IncomeFormat
import com.fintrack.kavindu.feature.income.viewmodel.IncomeInputValidator
import com.fintrack.kavindu.navigation.FreelanceProjectDetail
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

data class MilestoneRow(
    val id: String,
    val title: String,
    val amount: String, // in its own currency, e.g. "LKR 45,000" or "USD 200"
    val dueDate: String, // e.g. "24 Oct"
    val status: MilestoneStatus,
    val isOverdue: Boolean,
)

enum class DateTarget { DUE_DATE, RECEIVED_DATE }

data class FreelanceProjectDetailUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val clientName: String = "",
    val projectName: String = "",
    val pending: String? = null,
    val milestones: List<MilestoneRow> = emptyList(),
    // Add milestone dialog
    val isAddDialogVisible: Boolean = false,
    val titleInput: String = "",
    val amountInput: String = "",
    val currency: Currency = Currency.LKR,
    val dueDateText: String = "",
    val titleError: FreelanceFieldError? = null,
    val amountError: FreelanceFieldError? = null,
    // Mark received dialog
    val receivingMilestone: MilestoneRow? = null,
    val receivingIsForeign: Boolean = false,
    val receivedDateText: String = "",
    val lkrReceivedInput: String = "",
    val lkrReceivedError: IncomeFieldError? = null,
    val receivedDateError: IncomeFieldError? = null,
    // Cancel milestone dialog
    val cancellingMilestone: MilestoneRow? = null,
    // Date picker shared by both dialogs
    val datePickerTarget: DateTarget? = null,
    val datePickerMillis: Long = 0,
    val todayMillis: Long = 0,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
)

/**
 * One freelance project and its milestones (S1). "Mark received" turns a milestone into a linked income entry
 * in one save, so every payment matches its project and is counted exactly once (FR-12).
 */
@HiltViewModel
class FreelanceProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val freelanceRepository: FreelanceRepository,
) : ViewModel() {

    private val projectId: String = savedStateHandle.toRoute<FreelanceProjectDetail>().projectId

    private val _uiState = MutableStateFlow(FreelanceProjectDetailUiState())
    val uiState: StateFlow<FreelanceProjectDetailUiState> = _uiState.asStateFlow()

    private var project: FreelanceProject? = null
    private var milestones: List<Milestone> = emptyList()
    private var dueDate: LocalDate = LocalDate.now().plusWeeks(2)
    private var receivedDate: LocalDate = LocalDate.now()
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            combine(freelanceRepository.observeProjects(), freelanceRepository.observeMilestones(projectId)) { projects, items ->
                projects.firstOrNull { it.id == projectId } to items
            }
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { (found, items) ->
                    if (found == null) {
                        _uiState.update { it.copy(isLoading = false, loadError = AppError.INVALID_DATA) }
                        return@collect
                    }
                    project = found
                    milestones = items
                    val today = LocalDate.now()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            clientName = found.clientName,
                            projectName = found.projectName,
                            pending = FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(items)),
                            milestones = items.map { m -> m.toRow(today) },
                            todayMillis = today.toUtcMillis(),
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    // ---------- Add milestone ----------

    fun onAddMilestoneClicked() {
        dueDate = LocalDate.now().plusWeeks(2)
        _uiState.update {
            it.copy(
                isAddDialogVisible = true, titleInput = "", amountInput = "", currency = Currency.LKR,
                dueDateText = dueDate.format(DATE_FORMAT), titleError = null, amountError = null, saveError = null,
            )
        }
    }

    fun onAddMilestoneDismissed() = _uiState.update { it.copy(isAddDialogVisible = false) }

    fun onTitleChange(text: String) = _uiState.update { it.copy(titleInput = text, titleError = null) }

    fun onAmountChange(text: String) = _uiState.update { it.copy(amountInput = text, amountError = null) }

    fun onCurrencySelected(currency: Currency) = _uiState.update { it.copy(currency = currency) }

    fun onSaveMilestoneClicked() {
        val state = _uiState.value
        if (state.isSaving) return
        val check = FreelanceRules.validateMilestone(state.titleInput, state.amountInput)
        if (!check.isValid) {
            _uiState.update { it.copy(titleError = check.titleError, amountError = check.amountError) }
            return
        }
        val milestone = Milestone(
            id = "", // blank id = new document
            projectId = projectId,
            title = check.title,
            amountCents = check.amountCents!!,
            currency = state.currency,
            dueDate = dueDate,
            status = MilestoneStatus.PENDING,
            incomeId = null, // only a received milestone points to an income (security rules)
            createdAt = System.currentTimeMillis(),
        )
        save(closeDialogs = true) { freelanceRepository.saveMilestone(milestone) }
    }

    // ---------- Mark received ----------

    fun onMarkReceivedClicked(milestoneId: String) {
        val milestone = milestones.firstOrNull { it.id == milestoneId } ?: return
        receivedDate = LocalDate.now()
        _uiState.update {
            it.copy(
                receivingMilestone = milestone.toRow(LocalDate.now()),
                receivingIsForeign = milestone.currency != Currency.LKR,
                receivedDateText = receivedDate.format(DATE_FORMAT),
                lkrReceivedInput = "",
                lkrReceivedError = null,
                receivedDateError = null,
                saveError = null,
            )
        }
    }

    fun onMarkReceivedDismissed() = _uiState.update { it.copy(receivingMilestone = null) }

    fun onLkrReceivedChange(text: String) = _uiState.update { it.copy(lkrReceivedInput = text, lkrReceivedError = null) }

    fun onConfirmReceivedClicked() {
        val state = _uiState.value
        val row = state.receivingMilestone ?: return
        val milestone = milestones.firstOrNull { it.id == row.id } ?: return
        val currentProject = project ?: return
        if (state.isSaving) return

        // The same checks and exchange-rate rule as the Add income screen.
        val check = IncomeInputValidator.validate(
            IncomeFormInput(
                currency = milestone.currency,
                amountText = IncomeFormat.toInputText(milestone.amountCents),
                lkrReceivedText = state.lkrReceivedInput,
                date = receivedDate,
            ),
            LocalDate.now(),
        )
        if (!check.isValid) {
            _uiState.update { it.copy(lkrReceivedError = check.lkrReceivedError, receivedDateError = check.dateError) }
            return
        }
        val income = Income(
            id = "", // blank id = new document
            source = IncomeSource.FREELANCE,
            amountCents = check.amountCents!!,
            currency = milestone.currency,
            lkrAmountCents = check.lkrAmountCents!!,
            rateCentsPerUnit = check.rateCentsPerUnit!!,
            date = receivedDate,
            note = "${currentProject.projectName} · ${milestone.title}".take(MAX_NOTE_LENGTH),
            projectId = currentProject.id,
            milestoneId = milestone.id,
            createdAt = System.currentTimeMillis(),
        )
        save(closeDialogs = true) { freelanceRepository.markMilestoneReceived(milestone.id, income) }
    }

    // ---------- Cancel milestone ----------

    fun onCancelMilestoneClicked(milestoneId: String) {
        val milestone = milestones.firstOrNull { it.id == milestoneId } ?: return
        _uiState.update { it.copy(cancellingMilestone = milestone.toRow(LocalDate.now()), saveError = null) }
    }

    fun onCancelMilestoneDismissed() = _uiState.update { it.copy(cancellingMilestone = null) }

    fun onConfirmCancelClicked() {
        val row = _uiState.value.cancellingMilestone ?: return
        val milestone = milestones.firstOrNull { it.id == row.id } ?: return
        save(closeDialogs = true) { freelanceRepository.saveMilestone(milestone.copy(status = MilestoneStatus.CANCELLED)) }
    }

    // ---------- Date picker ----------

    fun onDueDateClicked() =
        _uiState.update { it.copy(datePickerTarget = DateTarget.DUE_DATE, datePickerMillis = dueDate.toUtcMillis()) }

    fun onReceivedDateClicked() =
        _uiState.update { it.copy(datePickerTarget = DateTarget.RECEIVED_DATE, datePickerMillis = receivedDate.toUtcMillis()) }

    fun onDatePickerDismissed() = _uiState.update { it.copy(datePickerTarget = null) }

    fun onDateSelected(utcMillis: Long?) {
        val target = _uiState.value.datePickerTarget
        val date = utcMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        _uiState.update { state ->
            when {
                date == null -> state.copy(datePickerTarget = null)
                target == DateTarget.DUE_DATE -> {
                    dueDate = date
                    state.copy(datePickerTarget = null, dueDateText = date.format(DATE_FORMAT))
                }
                else -> {
                    receivedDate = date
                    state.copy(datePickerTarget = null, receivedDateText = date.format(DATE_FORMAT), receivedDateError = null)
                }
            }
        }
    }

    private fun save(closeDialogs: Boolean, action: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = action()
            _uiState.update {
                if (result.isSuccess && closeDialogs) {
                    it.copy(isSaving = false, isAddDialogVisible = false, receivingMilestone = null, cancellingMilestone = null)
                } else {
                    it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
                }
            }
        }
    }

    private fun Milestone.toRow(today: LocalDate) = MilestoneRow(
        id = id,
        title = title,
        amount = IncomeFormat.amount(amountCents, currency),
        dueDate = dueDate.format(DAY_MONTH),
        status = status,
        isOverdue = FreelanceRules.isOverdue(this, today),
    )

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        const val MAX_NOTE_LENGTH = 200
    }
}
