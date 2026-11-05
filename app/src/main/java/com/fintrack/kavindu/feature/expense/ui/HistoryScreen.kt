package com.fintrack.kavindu.feature.expense.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.expense.viewmodel.DayKind
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryDay
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryRow
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryRowKind
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryType
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryUiState
import com.fintrack.kavindu.feature.expense.viewmodel.HistoryViewModel
import com.fintrack.kavindu.feature.income.ui.labelRes as incomeSourceLabelRes

/**
 * The History tab (M8): everything recorded in one pay cycle, newest first. Tap an expense to edit it, tap the bin to
 * delete it (with Undo), tap Confirm on a utility estimate. Income entries are listed too, so the cycle reads as one
 * timeline; tapping one opens its own screen. Shows the ViewModel's state and nothing else.
 */
@Composable
fun HistoryScreen(
    onExpenseClicked: (String) -> Unit,
    onIncomeClicked: (String) -> Unit,
    onRecurringClicked: () -> Unit,
    onCategoriesClicked: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Calm messages under the list: "Deleted. Undo", the quick-add confirmation, and any problem with an action.
    val deletedText = stringResource(R.string.history_deleted)
    val undoText = stringResource(R.string.history_undo)
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoClicked() else viewModel.onDeletedMessageShown()
    }
    val savedText = uiState.savedOutcome?.messageText()
    LaunchedEffect(uiState.savedOutcome) {
        if (savedText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(savedText)
        viewModel.onSavedMessageShown()
    }
    val errorText = uiState.actionError?.let { stringResource(it.messageRes()) }
    LaunchedEffect(uiState.actionError) {
        if (errorText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(errorText)
        viewModel.onActionErrorShown()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.history_title),
                subtitle = uiState.cycleLabel.ifEmpty { null },
                actions = {
                    IconButton(onClick = onRecurringClicked) {
                        Icon(imageVector = Icons.Filled.Repeat, contentDescription = stringResource(R.string.history_recurring))
                    }
                    IconButton(onClick = onCategoriesClicked) {
                        Icon(imageVector = Icons.Filled.Category, contentDescription = stringResource(R.string.history_categories))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddExpenseClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.history_add_expense)) },
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
            else -> HistoryContent(
                uiState = uiState,
                onExpenseClicked = onExpenseClicked,
                onIncomeClicked = onIncomeClicked,
                onPreviousCycleClicked = viewModel::onPreviousCycleClicked,
                onNextCycleClicked = viewModel::onNextCycleClicked,
                onDeleteClicked = viewModel::onDeleteClicked,
                onConfirmClicked = viewModel::onConfirmEstimateClicked,
                onAddExpenseClicked = viewModel::onAddExpenseClicked,
                onSearchChanged = viewModel::onSearchChanged,
                onTypeSelected = viewModel::onTypeSelected,
                onAllCyclesToggled = viewModel::onAllCyclesToggled,
                onClearFiltersClicked = viewModel::onClearFiltersClicked,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (uiState.isQuickAddVisible) {
        QuickAddSheet(onDismiss = viewModel::onQuickAddDismissed, onSaved = viewModel::onExpenseSaved)
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onExpenseClicked: (String) -> Unit,
    onIncomeClicked: (String) -> Unit,
    onPreviousCycleClicked: () -> Unit,
    onNextCycleClicked: () -> Unit,
    onDeleteClicked: (String) -> Unit,
    onConfirmClicked: (String) -> Unit,
    onAddExpenseClicked: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onTypeSelected: (HistoryType) -> Unit,
    onAllCyclesToggled: () -> Unit,
    onClearFiltersClicked: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CycleSwitcher(uiState, onPreviousCycleClicked, onNextCycleClicked)
        FilterBar(uiState, onSearchChanged, onTypeSelected, onAllCyclesToggled)
        if (uiState.isEmpty && uiState.isFiltered) {
            EmptyState(
                message = stringResource(R.string.history_no_matches),
                actionLabel = stringResource(R.string.history_clear_filters),
                onAction = onClearFiltersClicked,
                modifier = Modifier.weight(1f),
            )
        } else if (uiState.isEmpty) {
            EmptyState(
                message = stringResource(R.string.history_empty),
                actionLabel = stringResource(R.string.history_add_expense),
                onAction = onAddExpenseClicked,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                // Extra bottom space so the last entry is never hidden behind the Add expense button.
                contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                uiState.days.forEach { day ->
                    item(key = "day-${day.key}") { DayHeader(day) }
                    items(day.rows, key = { "${it.kind}-${it.id}" }) { row ->
                        HistoryRowCard(
                            row = row,
                            onClick = { if (row.kind == HistoryRowKind.INCOME) onIncomeClicked(row.id) else onExpenseClicked(row.id) },
                            onDeleteClicked = { onDeleteClicked(row.id) },
                            onConfirmClicked = { onConfirmClicked(row.id) },
                        )
                    }
                }
            }
        }
    }
}

// Previous and next pay cycle, with what was spent and received in the one on screen. Looking back is one tap.
@Composable
private fun CycleSwitcher(uiState: HistoryUiState, onPreviousClicked: () -> Unit, onNextClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // With "All cycles" on there is nothing to step through, so the arrows make room and disappear.
        if (uiState.isAllCycles) {
            Spacer(modifier = Modifier.size(48.dp))
        } else {
            IconButton(onClick = onPreviousClicked) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.history_previous_cycle))
            }
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.isAllCycles) stringResource(R.string.history_all_cycles) else uiState.cycleLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${stringResource(R.string.history_spent, uiState.spent)} · ${stringResource(R.string.history_received, uiState.received)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (uiState.isAllCycles) {
            Spacer(modifier = Modifier.size(48.dp))
        } else {
            IconButton(onClick = onNextClicked, enabled = !uiState.isCurrentCycle) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.history_next_cycle))
            }
        }
    }
}

// Search box and filter chips (E2). The list and the totals above follow them; nothing is saved.
@Composable
private fun FilterBar(
    uiState: HistoryUiState,
    onSearchChanged: (String) -> Unit,
    onTypeSelected: (HistoryType) -> Unit,
    onAllCyclesToggled: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onSearchChanged,
            placeholder = { Text(stringResource(R.string.history_search_hint)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.history_search_clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
        ) {
            FilterChip(
                selected = uiState.isAllCycles,
                onClick = onAllCyclesToggled,
                label = { Text(stringResource(R.string.history_all_cycles)) },
            )
            HistoryType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.type == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(stringResource(type.labelRes())) },
                )
            }
        }
    }
}

private fun HistoryType.labelRes(): Int = when (this) {
    HistoryType.ALL -> R.string.history_filter_all
    HistoryType.EXPENSES -> R.string.history_filter_expenses
    HistoryType.INCOME -> R.string.history_filter_income
    HistoryType.COMMITTED -> R.string.history_committed
    HistoryType.DISCRETIONARY -> R.string.history_discretionary
    HistoryType.UNRECORDED -> R.string.history_unrecorded
}

@Composable
private fun DayHeader(day: HistoryDay) {
    val label = when (day.kind) {
        DayKind.TODAY -> stringResource(R.string.history_today)
        DayKind.YESTERDAY -> stringResource(R.string.history_yesterday)
        DayKind.OTHER -> day.dateText
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HistoryRowCard(
    row: HistoryRow,
    onClick: () -> Unit,
    onDeleteClicked: () -> Unit,
    onConfirmClicked: () -> Unit,
) {
    val title = when (row.kind) {
        HistoryRowKind.UNRECORDED -> stringResource(R.string.history_unrecorded)
        HistoryRowKind.INCOME -> row.incomeSource?.let { stringResource(it.incomeSourceLabelRes()) }.orEmpty()
        HistoryRowKind.EXPENSE -> row.title ?: stringResource(R.string.history_expense_fallback)
    }
    val details = if (row.kind == HistoryRowKind.UNRECORDED) stringResource(R.string.history_unrecorded_detail) else row.details
    val tag = listOfNotNull(
        row.categoryType?.let { stringResource(it.labelRes()) },
        if (row.isEstimate) stringResource(R.string.history_estimate) else null,
    ).joinToString(" · ")

    StandardCard(tile = true, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (details.isNotEmpty()) {
                    Text(text = details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tag.isNotEmpty()) {
                    Text(text = tag, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = row.amount, style = MaterialTheme.typography.titleMedium)
                if (row.isEstimate) {
                    TextButton(onClick = onConfirmClicked) { Text(stringResource(R.string.history_confirm)) }
                }
            }
            if (row.canDelete) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.history_delete))
                }
            }
        }
    }
}
