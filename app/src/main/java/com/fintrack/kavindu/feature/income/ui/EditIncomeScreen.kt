package com.fintrack.kavindu.feature.income.ui

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.income.viewmodel.EditIncomeUiState
import com.fintrack.kavindu.feature.income.viewmodel.EditIncomeViewModel
import com.fintrack.kavindu.feature.income.viewmodel.IncomeFieldError
import com.fintrack.kavindu.core.ui.components.messageRes as appErrorMessageRes

/** Add a new income or edit one (M2). Shows the ViewModel's state and sends its events, nothing else. */
@Composable
fun EditIncomeScreen(
    onBackClicked: () -> Unit,
    onDone: () -> Unit,
    viewModel: EditIncomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Saved or deleted: go back to the list.
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (uiState.isEditing) R.string.income_edit_title else R.string.income_add_title),
                onBackClicked = onBackClicked,
                actions = {
                    if (uiState.isEditing && !uiState.isLinkedToProject && !uiState.isLoading && uiState.loadError == null) {
                        IconButton(onClick = viewModel::onDeleteClicked, enabled = !uiState.isSaving) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.income_delete))
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
            else -> EditIncomeForm(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onSourceSelected = viewModel::onSourceSelected,
                onCurrencySelected = viewModel::onCurrencySelected,
                onAmountChange = viewModel::onAmountChange,
                onLkrReceivedChange = viewModel::onLkrReceivedChange,
                onDateClicked = viewModel::onDateClicked,
                onNoteChange = viewModel::onNoteChange,
                onSaveClicked = viewModel::onSaveClicked,
            )
        }
    }

    if (uiState.isDatePickerVisible) {
        IncomeDatePicker(
            selectedMillis = uiState.dateMillis,
            maxMillis = uiState.maxDateMillis,
            onDateSelected = viewModel::onDateSelected,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }

    if (uiState.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.income_delete_title)) },
            text = { Text(stringResource(R.string.income_delete_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) { Text(stringResource(R.string.income_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) { Text(stringResource(R.string.income_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditIncomeForm(
    uiState: EditIncomeUiState,
    modifier: Modifier,
    onSourceSelected: (IncomeSource) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onAmountChange: (String) -> Unit,
    onLkrReceivedChange: (String) -> Unit,
    onDateClicked: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClicked: () -> Unit,
) {
    val enabled = !uiState.isSaving
    val canChangeSourceAndCurrency = enabled && !uiState.isLinkedToProject
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap),
    ) {
        // Source: one tap, Salary selected by default.
        SectionLabel(stringResource(R.string.income_source_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
        ) {
            IncomeSource.entries.forEach { source ->
                FilterChip(
                    selected = source == uiState.source,
                    onClick = { onSourceSelected(source) },
                    label = { Text(stringResource(source.labelRes())) },
                    enabled = canChangeSourceAndCurrency,
                )
            }
        }
        if (uiState.isLinkedToProject) {
            Text(
                text = stringResource(R.string.income_linked_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionLabel(stringResource(R.string.income_currency_label))
        CurrencySelector(selected = uiState.currency, enabled = canChangeSourceAndCurrency, onSelected = onCurrencySelected)

        AmountField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            label = stringResource(R.string.income_amount_label),
            prefix = uiState.currency.name,
            error = uiState.amountError,
            helper = null,
            enabled = enabled,
        )

        // Only for USD and USDT: the LKR that actually reached the bank. The rate is worked out from it.
        if (uiState.showsLkrReceived) {
            AmountField(
                value = uiState.lkrReceivedInput,
                onValueChange = onLkrReceivedChange,
                label = stringResource(R.string.income_lkr_received_label),
                prefix = Currency.LKR.name,
                error = uiState.lkrReceivedError,
                helper = stringResource(R.string.income_lkr_received_help),
                enabled = enabled,
            )
            uiState.ratePreview?.let { rate ->
                Text(
                    text = stringResource(R.string.income_rate_preview, uiState.currency.name, rate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column {
            SectionLabel(stringResource(R.string.income_date_label))
            OutlinedButton(onClick = onDateClicked, enabled = enabled, modifier = Modifier.padding(top = 8.dp)) {
                Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = uiState.dateText, modifier = Modifier.padding(start = 8.dp))
            }
            uiState.dateError?.let { FieldError(it) }
        }

        OutlinedTextField(
            value = uiState.noteInput,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.income_note_label)) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.appErrorMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onSaveClicked, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.income_save))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelector(selected: Currency, enabled: Boolean, onSelected: (Currency) -> Unit) {
    val options = Currency.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, currency ->
            SegmentedButton(
                selected = currency == selected,
                onClick = { onSelected(currency) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                enabled = enabled,
            ) {
                Text(currency.name)
            }
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String,
    error: IncomeFieldError?,
    helper: String?,
    enabled: Boolean,
) {
    val supporting: String? = if (error != null) stringResource(error.messageRes()) else helper
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("$prefix ") },
        singleLine = true,
        enabled = enabled,
        isError = error != null, // validation errors are the one place red is allowed (UI spec, section 1)
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FieldError(error: IncomeFieldError) {
    Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeDatePicker(
    selectedMillis: Long,
    maxMillis: Long,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = object : SelectableDates {
            // Future days are greyed out: income is recorded when it arrives.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) { Text(stringResource(R.string.income_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.income_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
