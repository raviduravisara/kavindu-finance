package com.fintrack.kavindu.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.expense.viewmodel.EditExpenseUiState
import com.fintrack.kavindu.feature.expense.viewmodel.EditExpenseViewModel
import com.fintrack.kavindu.feature.expense.viewmodel.ExpenseFieldError

/** Edit or delete one expense (M8). A form screen like the others: shows the ViewModel's state and sends its events. */
@Composable
fun EditExpenseScreen(
    onBackClicked: () -> Unit,
    onDone: () -> Unit,
    viewModel: EditExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Saved or deleted: go back to history.
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.editexpense_title),
                onBackClicked = onBackClicked,
                actions = {
                    if (uiState.canDelete && !uiState.isLoading && uiState.loadError == null) {
                        IconButton(onClick = viewModel::onDeleteClicked, enabled = !uiState.isSaving) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.editexpense_delete))
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
                message = stringResource(loadError.messageRes()),
                onRetry = viewModel::onRetryClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> EditExpenseForm(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onAmountChange = viewModel::onAmountChange,
                onCategorySelected = viewModel::onCategorySelected,
                onChannelSelected = viewModel::onChannelSelected,
                onAccountSelected = viewModel::onAccountSelected,
                onDateClicked = viewModel::onDateClicked,
                onNoteChange = viewModel::onNoteChange,
                onSaveClicked = viewModel::onSaveClicked,
            )
        }
    }

    if (uiState.isDatePickerVisible) {
        ExpenseDatePicker(
            selectedMillis = uiState.dateMillis,
            maxMillis = uiState.todayMillis,
            onDateSelected = viewModel::onDateSelected,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }

    if (uiState.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.editexpense_delete_title)) },
            text = { Text(stringResource(R.string.editexpense_delete_text)) },
            confirmButton = { TextButton(onClick = viewModel::onDeleteConfirmed) { Text(stringResource(R.string.editexpense_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismissed) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditExpenseForm(
    uiState: EditExpenseUiState,
    modifier: Modifier,
    onAmountChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onChannelSelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDateClicked: () -> Unit,
    onNoteChange: (String) -> Unit,
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
        if (uiState.isEstimate) HelperText(stringResource(R.string.editexpense_estimate_note))
        if (uiState.isUnrecorded) HelperText(stringResource(R.string.editexpense_unrecorded_note))

        val amountError = uiState.amountError
        OutlinedTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.editexpense_amount_label)) },
            prefix = { Text("LKR ") },
            singleLine = true,
            enabled = enabled,
            isError = amountError != null, // validation errors are the one place red is allowed (UI spec, section 1)
            supportingText = amountError?.let { { Text(stringResource(it.errorRes())) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(stringResource(R.string.editexpense_category_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
        ) {
            uiState.categories.forEach { category ->
                FilterChip(
                    selected = category.id == uiState.selectedCategoryId,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                    enabled = enabled,
                )
            }
        }
        uiState.categoryError?.let { FieldError(it) }

        SectionLabel(stringResource(R.string.editexpense_channel_label))
        ChipRow(options = uiState.channels, selected = uiState.channel, enabled = enabled, onSelected = onChannelSelected)

        SectionLabel(stringResource(R.string.editexpense_account_label))
        ChipRow(options = uiState.accounts, selected = uiState.account, enabled = enabled, onSelected = onAccountSelected)

        Column {
            SectionLabel(stringResource(R.string.editexpense_date_label))
            OutlinedButton(
                onClick = onDateClicked,
                enabled = enabled && !uiState.isDateLocked,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = uiState.dateText, modifier = Modifier.padding(start = 8.dp))
            }
            if (uiState.isDateLocked) HelperText(stringResource(R.string.editexpense_date_locked))
            uiState.dateError?.let { FieldError(it) }
        }

        val noteError = uiState.noteError
        OutlinedTextField(
            value = uiState.noteInput,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.editexpense_note_label)) },
            singleLine = true,
            enabled = enabled,
            isError = noteError != null,
            supportingText = noteError?.let { { Text(stringResource(it.errorRes())) } },
            modifier = Modifier.fillMaxWidth(),
        )

        if (!uiState.canDelete && uiState.isDateLocked) HelperText(stringResource(R.string.editexpense_cannot_delete))

        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onSaveClicked, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.editexpense_save))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, enabled: Boolean, onSelected: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
    ) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelected(option) }, label = { Text(option) }, enabled = enabled)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HelperText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FieldError(error: ExpenseFieldError) {
    Text(
        text = stringResource(error.errorRes()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error, // a form validation error: the one place red is allowed
        modifier = Modifier.padding(top = 4.dp),
    )
}
