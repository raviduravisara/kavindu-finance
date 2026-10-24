package com.fintrack.kavindu.feature.goal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.goal.viewmodel.EditGoalUiState
import com.fintrack.kavindu.feature.goal.viewmodel.EditGoalViewModel
import com.fintrack.kavindu.feature.goal.viewmodel.GoalFieldError
import com.fintrack.kavindu.core.ui.components.messageRes as appErrorMessageRes

/** Set or change the savings goal (FR-36). Shows the ViewModel's state and sends its events, nothing else. */
@Composable
fun EditGoalScreen(
    onBackClicked: () -> Unit,
    onDone: () -> Unit,
    viewModel: EditGoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (uiState.isEditing) R.string.goal_edit_title else R.string.goal_set_title),
                onBackClicked = onBackClicked,
                actions = {
                    if (uiState.isEditing && !uiState.isLoading) {
                        IconButton(onClick = viewModel::onDeleteClicked, enabled = !uiState.isSaving) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.goal_delete))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val loadError = uiState.loadError
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            loadError != null -> ErrorState(
                message = stringResource(loadError.appErrorMessageRes()),
                onRetry = viewModel::onRetryClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> EditGoalForm(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onNameChange = viewModel::onNameChange,
                onTargetChange = viewModel::onTargetChange,
                onStartingSavingsChange = viewModel::onStartingSavingsChange,
                onDeadlineClicked = viewModel::onDeadlineClicked,
                onSaveClicked = viewModel::onSaveClicked,
            )
        }
    }

    if (uiState.isDatePickerVisible) {
        DeadlinePicker(
            selectedMillis = uiState.deadlineMillis,
            minMillis = uiState.minDeadlineMillis,
            onDateSelected = viewModel::onDeadlineSelected,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }

    if (uiState.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_text)) },
            confirmButton = { TextButton(onClick = viewModel::onDeleteConfirmed) { Text(stringResource(R.string.goal_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismissed) { Text(stringResource(R.string.goal_cancel)) } },
        )
    }
}

@Composable
private fun EditGoalForm(
    uiState: EditGoalUiState,
    modifier: Modifier,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onStartingSavingsChange: (String) -> Unit,
    onDeadlineClicked: () -> Unit,
    onSaveClicked: () -> Unit,
) {
    val enabled = !uiState.isSaving
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap),
    ) {
        GoalTextField(
            value = uiState.nameInput,
            onValueChange = onNameChange,
            label = stringResource(R.string.goal_name_label),
            placeholder = stringResource(R.string.goal_name_placeholder),
            error = uiState.nameError,
            enabled = enabled,
            isAmount = false,
        )
        GoalTextField(
            value = uiState.targetInput,
            onValueChange = onTargetChange,
            label = stringResource(R.string.goal_target_label),
            placeholder = null,
            error = uiState.targetError,
            enabled = enabled,
            isAmount = true,
        )
        GoalTextField(
            value = uiState.startingSavingsInput,
            onValueChange = onStartingSavingsChange,
            label = stringResource(R.string.goal_starting_label),
            placeholder = stringResource(R.string.goal_starting_placeholder),
            error = uiState.startingError,
            enabled = enabled,
            isAmount = true,
        )

        Column {
            Text(
                text = stringResource(R.string.goal_deadline_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onDeadlineClicked, enabled = enabled, modifier = Modifier.padding(top = 8.dp)) {
                Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = uiState.deadlineText, modifier = Modifier.padding(start = 8.dp))
            }
            uiState.deadlineError?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // Turns the big number into this cycle's number straight away (RC4).
        uiState.perCycleHint?.let { perCycle ->
            Text(
                text = stringResource(R.string.goal_per_cycle_hint, perCycle, uiState.cyclesHint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.appErrorMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onSaveClicked, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.goal_save))
        }
    }
}

@Composable
private fun GoalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    error: GoalFieldError?,
    enabled: Boolean,
    isAmount: Boolean,
) {
    val errorText: String? = if (error != null) stringResource(error.messageRes()) else null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        prefix = if (isAmount) {
            { Text("LKR ") }
        } else {
            null
        },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null, // validation errors are the one place red is allowed
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (isAmount) KeyboardType.Decimal else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(
    selectedMillis: Long,
    minMillis: Long,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = object : SelectableDates {
            // Only future days: a deadline today or in the past makes no sense.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) { Text(stringResource(R.string.goal_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.goal_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
