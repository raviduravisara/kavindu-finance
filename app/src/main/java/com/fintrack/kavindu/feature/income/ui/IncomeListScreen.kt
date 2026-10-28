package com.fintrack.kavindu.feature.income.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.HeroCard
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.PromptCard
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.income.viewmodel.IncomeListUiState
import com.fintrack.kavindu.feature.income.viewmodel.IncomeListViewModel
import com.fintrack.kavindu.feature.income.viewmodel.IncomeRow
import com.fintrack.kavindu.feature.income.viewmodel.SourceTotal

/** The Income tab: what actually arrived this pay cycle, in total, per source and entry by entry (FR-10). */
@Composable
fun IncomeListScreen(
    onAddIncomeClicked: () -> Unit,
    onIncomeClicked: (String) -> Unit,
    onFreelanceClicked: () -> Unit,
    onCryptoClicked: () -> Unit,
    viewModel: IncomeListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.income_list_title),
                subtitle = uiState.cycleLabel.ifEmpty { null },
                actions = {
                    IconButton(onClick = onFreelanceClicked) {
                        Icon(imageVector = Icons.Filled.Work, contentDescription = stringResource(R.string.freelance_title))
                    }
                    IconButton(onClick = onCryptoClicked) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart, contentDescription = stringResource(R.string.crypto_title))
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = onAddIncomeClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.income_add_title)) },
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
            uiState.isEmpty -> EmptyState(
                message = stringResource(R.string.income_empty),
                actionLabel = stringResource(R.string.income_add_title),
                onAction = onAddIncomeClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> IncomeListContent(uiState, onIncomeClicked, onFreelanceClicked, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun IncomeListContent(
    uiState: IncomeListUiState,
    onIncomeClicked: (String) -> Unit,
    onFreelanceClicked: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Extra bottom space so the last entry is never hidden behind the Add income button.
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        item { ReceivedCard(uiState.totalReceived) }
        if (uiState.freelancePending != null || uiState.overdueInvoices > 0) {
            item { FreelanceCard(uiState.freelancePending, uiState.overdueInvoices, onFreelanceClicked) }
        }
        if (uiState.bySource.size > 1) {
            item { BySourceCard(uiState.bySource) }
        }
        items(uiState.rows, key = { it.id }) { row ->
            IncomeRowCard(row = row, onClick = { onIncomeClicked(row.id) })
        }
    }
}

// Measured income, not his estimate: only money that has arrived counts.
@Composable
private fun ReceivedCard(total: String) {
    HeroCard {
        Text(text = stringResource(R.string.income_received_label), style = MaterialTheme.typography.labelLarge)
        Text(text = total, style = MaterialTheme.typography.displaySmall)
        Text(text = stringResource(R.string.income_received_help), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BySourceCard(totals: List<SourceTotal>) {
    StandardCard {
        Text(text = stringResource(R.string.income_by_source), style = MaterialTheme.typography.titleMedium)
        totals.forEach { total ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(total.source.labelRes()), style = MaterialTheme.typography.bodyMedium)
                Text(text = total.total, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun IncomeRowCard(row: IncomeRow, onClick: () -> Unit) {
    StandardCard(tile = true, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(row.source.labelRes()), style = MaterialTheme.typography.bodyLarge)
            Text(text = row.lkrAmount, style = MaterialTheme.typography.titleMedium)
        }
        val converted = if (row.original != null && row.rate != null) {
            stringResource(R.string.income_row_converted, row.original, row.currency.name, row.rate)
        } else {
            null
        }
        val details = listOfNotNull(row.date, converted, row.note)
        Text(
            text = details.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Money owed by clients is shown next to income but never added to it (FR-14). Tapping opens the projects.
@Composable
private fun FreelanceCard(pending: String?, overdue: Int, onClick: () -> Unit) {
    PromptCard(onClick = onClick) {
        Text(text = stringResource(R.string.freelance_card_title), style = MaterialTheme.typography.titleMedium)
        if (pending != null) {
            Text(text = stringResource(R.string.freelance_card_pending, pending), style = MaterialTheme.typography.bodyMedium)
        }
        if (overdue > 0) {
            Text(
                text = if (overdue == 1) stringResource(R.string.freelance_overdue_one)
                else stringResource(R.string.freelance_overdue_many, overdue),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
