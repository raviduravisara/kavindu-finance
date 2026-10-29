package com.fintrack.kavindu.feature.goal.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.fintrack.kavindu.core.data.repository.GoalRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.Goal
import com.fintrack.kavindu.core.model.asAppError
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.navigation.EditGoal
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class EditGoalUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val isEditing: Boolean = false, // false = setting the first goal
    val nameInput: String = "",
    val targetInput: String = "",
    val startingSavingsInput: String = "",
    val deadlineText: String = "", // e.g. "24 Oct 2027"
    val deadlineMillis: Long = 0, // for the date picker (UTC midnight)
    val minDeadlineMillis: Long = 0, // the first day that can be picked (tomorrow)
    val isDatePickerVisible: Boolean = false,
    val perCycleHint: String? = null, // e.g. "LKR 39,900" a cycle over 12 cycles, shown while typing
    val cyclesHint: Int = 0,
    val nameError: GoalFieldError? = null,
    val targetError: GoalFieldError? = null,
    val startingError: GoalFieldError? = null,
    val deadlineError: GoalFieldError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDone: Boolean = false, // saved or deleted: the screen closes
)

/**
 * Set or change the savings goal (FR-36). One active goal, as in the GoalRepository.
 * A new goal starts counting today; editing keeps the original start date, so "saved so far" does not reset.
 * "Set a new goal" (after a goal is reached) opens an empty form and starts counting again from today.
 * It reuses the same document and its createdAt, because the security rules never let createdAt change.
 */
@HiltViewModel
class EditGoalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val startNew: Boolean = savedStateHandle.toRoute<EditGoal>().startNew

    private val _uiState = MutableStateFlow(EditGoalUiState())
    val uiState: StateFlow<EditGoalUiState> = _uiState.asStateFlow()

    private var original: Goal? = null
    private var deadline: LocalDate = LocalDate.now().plusYears(1)

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val goal = goalRepository.observeGoal().first()
                original = goal
                if (goal != null && !startNew) {
                    deadline = goal.deadline
                    _uiState.update {
                        it.copy(
                            isEditing = true,
                            nameInput = goal.name,
                            targetInput = goal.targetCents.toInputText(),
                            startingSavingsInput = goal.startingSavingsCents.toInputText(),
                        )
                    }
                }
                setDeadline(deadline)
                refreshHint()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = e.asAppError()) }
            }
        }
    }

    fun onRetryClicked() = load()

    fun onNameChange(text: String) =
        _uiState.update { it.copy(nameInput = text.take(GoalFormValidator.MAX_NAME_LENGTH), nameError = null) }

    fun onTargetChange(text: String) {
        _uiState.update { it.copy(targetInput = text, targetError = null) }
        refreshHint()
    }

    fun onStartingSavingsChange(text: String) {
        _uiState.update { it.copy(startingSavingsInput = text, startingError = null) }
        refreshHint()
    }

    fun onDeadlineClicked() = _uiState.update { it.copy(isDatePickerVisible = true) }

    fun onDatePickerDismissed() = _uiState.update { it.copy(isDatePickerVisible = false) }

    fun onDeadlineSelected(utcMillis: Long?) {
        if (utcMillis != null) setDeadline(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())
        _uiState.update { it.copy(isDatePickerVisible = false, deadlineError = null) }
        refreshHint()
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (state.isSaving) return
        val today = LocalDate.now()
        val check = GoalFormValidator.validate(
            GoalFormInput(state.nameInput, state.targetInput, state.startingSavingsInput, deadline),
            today,
        )
        if (!check.isValid) {
            _uiState.update {
                it.copy(
                    nameError = check.nameError,
                    targetError = check.targetError,
                    startingError = check.startingError,
                    deadlineError = check.deadlineError,
                )
            }
            return
        }
        val old = original
        val goal = Goal(
            id = old?.id ?: "", // blank id = new document
            name = check.name,
            targetCents = check.targetCents!!,
            deadline = deadline,
            startingSavingsCents = check.startingSavingsCents!!,
            startDate = if (old == null || startNew) today else old.startDate,
            createdAt = old?.createdAt ?: System.currentTimeMillis(),
        )
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = goalRepository.saveGoal(goal)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isDone = true)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    fun onDeleteClicked() = _uiState.update { it.copy(isDeleteDialogVisible = true) }

    fun onDeleteDismissed() = _uiState.update { it.copy(isDeleteDialogVisible = false) }

    fun onDeleteConfirmed() {
        val id = original?.id ?: return
        _uiState.update { it.copy(isDeleteDialogVisible = false, isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = goalRepository.deleteGoal(id)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isDone = true)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    private fun setDeadline(date: LocalDate) {
        deadline = date
        _uiState.update {
            it.copy(
                deadlineText = date.format(DATE_FORMAT),
                deadlineMillis = date.toUtcMillis(),
                minDeadlineMillis = LocalDate.now().plusDays(1).toUtcMillis(),
            )
        }
    }

    // While typing: "That is about LKR 39,900 a cycle over 12 cycles", so the goal feels real at once (RC4).
    private fun refreshHint() {
        val state = _uiState.value
        val target = Money.parseToCents(state.targetInput)?.takeIf { it > 0 }
        val starting = if (state.startingSavingsInput.isBlank()) 0L else Money.parseToCents(state.startingSavingsInput)
        val today = LocalDate.now()
        val cycles = (ChronoUnit.MONTHS.between(today.withDayOfMonth(1), deadline.withDayOfMonth(1)).toInt())
            .coerceAtLeast(1)
        val hint = if (target != null && starting != null && target > starting && deadline.isAfter(today)) {
            val remaining = target - starting
            Money.formatLkr((remaining + cycles - 1) / cycles)
        } else {
            null
        }
        _uiState.update { it.copy(perCycleHint = hint, cyclesHint = cycles) }
    }

    private fun Long.toInputText(): String {
        val whole = this / 100
        val fraction = this % 100
        return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
    }

    private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
