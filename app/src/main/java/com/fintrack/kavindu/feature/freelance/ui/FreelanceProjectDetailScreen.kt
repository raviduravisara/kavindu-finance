package com.fintrack.kavindu.feature.freelance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.PromptCard
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.freelance.viewmodel.DateTarget
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceProjectDetailUiState
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceProjectDetailViewModel
import com.fintrack.kavindu.feature.freelance.viewmodel.MilestoneRow
import com.fintrack.kavindu.core.ui.components.messageRes as appErrorMessageRes
import com.fintrack.kavindu.feature.income.ui.messageRes as incomeErrorMessageRes

/** One freelance project: its milestones, with "Mark received" creating the linked income (FR-11, FR-12). */
@Composable
fun FreelanceProjectDetailScreen(
    onBackClicked: () -> Unit,
    viewModel: FreelanceProjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.projectName.ifEmpty { stringResource(R.string.freelance_title) },
                subtitle = uiState.clientName.ifEmpty { null },
                onBackClicked = onBackClicked,
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddMilestoneClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.freelance_add_milestone)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
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
            uiState.milestones.isEmpty() -> EmptyState(
                message = stringResource(R.string.freelance_no_milestones),
                actionLabel = stringResource(R.string.freelance_add_milestone),
                onAction = viewModel::onAddMilestoneClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                item {
                    Text(
                        text = uiState.pending?.let { stringResource(R.string.freelance_project_pending, it) }
                            ?: stringResource(R.string.freelance_paid_up),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(uiState.milestones, key = { it.id }) { row ->
                    MilestoneCard(
                        row = row,
                        enabled = !uiState.isSaving,
                        onMarkReceived = { viewModel.onMarkReceivedClicked(row.id) },
                        onCancel = { viewModel.onCancelMilestoneClicked(row.id) },
                    )
                }
            }
        }
    }

    if (uiState.isAddDialogVisible) AddMilestoneDialog(uiState, viewModel)
    if (uiState.receivingMilestone != null) MarkReceivedDialog(uiState, viewModel)
    uiState.cancellingMilestone?.let { row ->
        AlertDialog(
            onDismissRequest = viewModel::onCancelMilestoneDismissed,
            title = { Text(stringResource(R.string.freelance_cancel_title)) },
            text = { Text(stringResource(R.string.freelance_cancel_text, row.title)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmCancelClicked, enabled = !uiState.isSaving) {
                    Text(stringResource(R.string.freelance_cancel_confirm))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onCancelMilestoneDismissed) { Text(stringResource(R.string.freelance_keep)) } },
        )
    }
    uiState.datePickerTarget?.let { target ->
        MilestoneDatePicker(
            selectedMillis = uiState.datePickerMillis,
            // A received payment cannot be in the future; a due date can be any day.
            maxMillis = if (target == DateTarget.RECEIVED_DATE) uiState.todayMillis else null,
            onDateSelected = viewModel::onDateSelected,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }
}

// Overdue milestones use the gentle prompt style with "follow up" wording, never red (NFR-09).
@Composable
private fun MilestoneCard(row: MilestoneRow, enabled: Boolean, onMarkReceived: () -> Unit, onCancel: () -> Unit) {
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = row.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = row.amount, style = MaterialTheme.typography.titleMedium)
        }
        val statusText = when {
            row.isOverdue -> stringResource(R.string.freelance_overdue_detail, row.dueDate)
            row.status == MilestoneStatus.PENDING -> stringResource(R.string.freelance_due, row.dueDate)
            row.status == MilestoneStatus.RECEIVED -> stringResource(R.string.freelance_received_detail)
            else -> stringResource(row.status.labelRes())
        }
        Text(text = statusText, style = MaterialTheme.typography.bodySmall)
        if (row.status == MilestoneStatus.PENDING) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel, enabled = enabled) { Text(stringResource(R.string.freelance_cancel_milestone)) }
                FilledTonalButton(onClick = onMarkReceived, enabled = enabled) { Text(stringResource(R.string.freelance_mark_received)) }
            }
        }
    }
    if (row.isOverdue) PromptCard { content() } else StandardCard(tile = true) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMilestoneDialog(uiState: FreelanceProjectDetailUiState, viewModel: FreelanceProjectDetailViewModel) {
    val enabled = !uiState.isSaving
    val amountErrorText: String? = uiState.amountError?.let { stringResource(it.messageRes()) }
    AlertDialog(
        onDismissRequest = viewModel::onAddMilestoneDismissed,
        title = { Text(stringResource(R.string.freelance_add_milestone)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogTextField(uiState.titleInput, viewModel::onTitleChange, stringResource(R.string.freelance_milestone_label), uiState.titleError, enabled)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
                    Currency.entries.forEach { currency ->
                        FilterChip(
                            selected = currency == uiState.currency,
                            onClick = { viewModel.onCurrencySelected(currency) },
                            label = { Text(currency.name) },
                            enabled = enabled,
                        )
                    }
                }
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.freelance_amount_label)) },
                    prefix = { Text("${uiState.currency.name} ") },
                    singleLine = true,
                    enabled = enabled,
                    isError = amountErrorText != null,
                    supportingText = if (amountErrorText != null) {
                        { Text(amountErrorText) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateButton(label = stringResource(R.string.freelance_due_label), text = uiState.dueDateText, enabled = enabled, onClick = viewModel::onDueDateClicked)
                uiState.saveError?.let { Text(stringResource(it.appErrorMessageRes()), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::onSaveMilestoneClicked, enabled = enabled) { Text(stringResource(R.string.freelance_save)) } },
        dismissButton = { TextButton(onClick = viewModel::onAddMilestoneDismissed) { Text(stringResource(R.string.freelance_cancel)) } },
    )
}

@Composable
private fun MarkReceivedDialog(uiState: FreelanceProjectDetailUiState, viewModel: FreelanceProjectDetailViewModel) {
    val row = uiState.receivingMilestone ?: return
    val enabled = !uiState.isSaving
    val lkrErrorText: String? = uiState.lkrReceivedError?.let { stringResource(it.incomeErrorMessageRes()) }
    AlertDialog(
        onDismissRequest = viewModel::onMarkReceivedDismissed,
        title = { Text(stringResource(R.string.freelance_mark_received)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.freelance_receive_text, row.title, row.amount), style = MaterialTheme.typography.bodyMedium)
                DateButton(label = stringResource(R.string.freelance_received_on), text = uiState.receivedDateText, enabled = enabled, onClick = viewModel::onReceivedDateClicked)
                uiState.receivedDateError?.let {
                    Text(stringResource(it.incomeErrorMessageRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                // Foreign currency: the LKR that actually reached the bank, exactly as on the Add income screen.
                if (uiState.receivingIsForeign) {
                    OutlinedTextField(
                        value = uiState.lkrReceivedInput,
                        onValueChange = viewModel::onLkrReceivedChange,
                        label = { Text(stringResource(R.string.income_lkr_received_label)) },
                        prefix = { Text("LKR ") },
                        singleLine = true,
                        enabled = enabled,
                        isError = lkrErrorText != null,
                        supportingText = { Text(lkrErrorText ?: stringResource(R.string.income_lkr_received_help)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(text = stringResource(R.string.freelance_receive_note), style = MaterialTheme.typography.bodySmall)
                uiState.saveError?.let { Text(stringResource(it.appErrorMessageRes()), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::onConfirmReceivedClicked, enabled = enabled) { Text(stringResource(R.string.freelance_mark_received)) } },
        dismissButton = { TextButton(onClick = viewModel::onMarkReceivedDismissed) { Text(stringResource(R.string.freelance_cancel)) } },
    )
}

@Composable
private fun DateButton(label: String, text: String, enabled: Boolean, onClick: () -> Unit) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(top = 4.dp)) {
            Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneDatePicker(
    selectedMillis: Long,
    maxMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = maxMillis == null || utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) { Text(stringResource(R.string.freelance_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.freelance_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
