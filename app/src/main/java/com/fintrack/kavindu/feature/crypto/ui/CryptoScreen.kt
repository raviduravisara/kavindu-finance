package com.fintrack.kavindu.feature.crypto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.HeroCard
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.crypto.viewmodel.CryptoUiState
import com.fintrack.kavindu.feature.crypto.viewmodel.CryptoViewModel
import com.fintrack.kavindu.feature.crypto.viewmodel.TransferRow

/** The crypto card (S2): net position from money moved in and out of Binance. Displays state only. */
@Composable
fun CryptoScreen(
    onBackClicked: () -> Unit,
    viewModel: CryptoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.crypto_title), onBackClicked = onBackClicked) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddTransferClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.crypto_add_transfer)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
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
            else -> CryptoContent(uiState, viewModel::onDeleteClicked, Modifier.padding(innerPadding))
        }
    }

    if (uiState.isAddDialogVisible) AddTransferDialog(uiState, viewModel)
    if (uiState.isDatePickerVisible) TransferDatePicker(uiState.dateMillis, uiState.todayMillis, viewModel::onDateSelected, viewModel::onDatePickerDismissed)
    uiState.deletingRow?.let { row ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.crypto_delete_title)) },
            text = { Text(stringResource(R.string.crypto_delete_text, row.amount, row.date)) },
            confirmButton = { TextButton(onClick = viewModel::onDeleteConfirmed, enabled = !uiState.isSaving) { Text(stringResource(R.string.crypto_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismissed) { Text(stringResource(R.string.crypto_cancel)) } },
        )
    }
}

@Composable
private fun CryptoContent(uiState: CryptoUiState, onDeleteClicked: (String) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        // Net position: a negative number is shown calmly, as it is (no red).
        item {
            HeroCard {
                Text(text = stringResource(R.string.crypto_net_position), style = MaterialTheme.typography.labelLarge)
                Text(text = uiState.net, style = MaterialTheme.typography.displaySmall)
                Text(
                    text = stringResource(if (uiState.isNetNegative) R.string.crypto_net_negative else R.string.crypto_net_help),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StandardCard(tile = true, modifier = Modifier.weight(1f)) {
                    TileLabel(stringResource(R.string.crypto_moved_in))
                    Text(text = uiState.totalIn, style = MaterialTheme.typography.titleLarge)
                }
                StandardCard(tile = true, modifier = Modifier.weight(1f)) {
                    TileLabel(stringResource(R.string.crypto_moved_out))
                    Text(text = uiState.totalOut, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        // Only profit that has come back out counts as income, so it is never counted twice.
        item {
            StandardCard {
                Text(text = stringResource(R.string.crypto_counted_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.crypto_counted_cycle, uiState.realisedThisCycle), style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(R.string.crypto_counted_total, uiState.realisedTotal), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(R.string.crypto_counted_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.crypto_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(uiState.rows, key = { it.id }) { row -> TransferCard(row, onDelete = { onDeleteClicked(row.id) }) }
    }
}

@Composable
private fun TransferCard(row: TransferRow, onDelete: () -> Unit) {
    StandardCard(tile = true) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(row.direction.labelRes()), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOfNotNull(row.date, row.note).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = row.amount, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.crypto_delete))
            }
        }
    }
}

@Composable
private fun AddTransferDialog(uiState: CryptoUiState, viewModel: CryptoViewModel) {
    val enabled = !uiState.isSaving
    val amountError: String? = if (uiState.isAmountInvalid) stringResource(R.string.crypto_error_amount) else null
    AlertDialog(
        onDismissRequest = viewModel::onAddTransferDismissed,
        title = { Text(stringResource(R.string.crypto_add_transfer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
                    CryptoDirection.entries.forEach { direction ->
                        FilterChip(
                            selected = direction == uiState.direction,
                            onClick = { viewModel.onDirectionSelected(direction) },
                            label = { Text(stringResource(direction.labelRes())) },
                            enabled = enabled,
                        )
                    }
                }
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.crypto_amount_label)) },
                    prefix = { Text("LKR ") },
                    singleLine = true,
                    enabled = enabled,
                    isError = amountError != null,
                    supportingText = if (amountError != null) {
                        { Text(amountError) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = viewModel::onDateClicked, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text = uiState.dateText, modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedTextField(
                    value = uiState.noteInput,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(stringResource(R.string.crypto_note_label)) },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                uiState.saveError?.let { Text(stringResource(it.messageRes()), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::onSaveTransferClicked, enabled = enabled) { Text(stringResource(R.string.crypto_save)) } },
        dismissButton = { TextButton(onClick = viewModel::onAddTransferDismissed) { Text(stringResource(R.string.crypto_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferDatePicker(selectedMillis: Long, maxMillis: Long, onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) { Text(stringResource(R.string.crypto_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.crypto_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun TileLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun CryptoDirection.labelRes(): Int = when (this) {
    CryptoDirection.INTO_BINANCE -> R.string.crypto_into
    CryptoDirection.OUT_OF_BINANCE -> R.string.crypto_out_of
}
