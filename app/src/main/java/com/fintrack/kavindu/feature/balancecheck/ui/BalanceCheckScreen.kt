package com.fintrack.kavindu.feature.balancecheck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.feature.balancecheck.viewmodel.BalanceCheckUiState
import com.fintrack.kavindu.feature.balancecheck.viewmodel.BalanceCheckViewModel
import com.fintrack.kavindu.feature.balancecheck.viewmodel.CheckResult
import com.fintrack.kavindu.feature.balancecheck.viewmodel.GapKind
import com.fintrack.kavindu.feature.balancecheck.viewmodel.LastCheckInfo

@Composable
fun BalanceCheckScreen(
    onBackClicked: () -> Unit,
    viewModel: BalanceCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.balance_title), onBackClicked = onBackClicked) },
    ) { innerPadding ->
        val loadError = uiState.loadError
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            loadError != null -> ErrorState(
                message = stringResource(loadError.messageRes()),
                onRetry = viewModel::onRetryClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> BalanceCheckContent(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onBankBalanceChange = viewModel::onBankBalanceChange,
                onWalletCashChange = viewModel::onWalletCashChange,
                onSaveStartingPointClicked = viewModel::onSaveStartingPointClicked,
                onCheckClicked = viewModel::onCheckClicked,
                onSaveCheckClicked = viewModel::onSaveCheckClicked,
                onAddGapAsIncomeClicked = viewModel::onAddGapAsIncomeClicked,
                onIgnoreGapClicked = viewModel::onIgnoreGapClicked,
            )
        }
    }
}

@Composable
private fun BalanceCheckContent(
    uiState: BalanceCheckUiState,
    modifier: Modifier,
    onBankBalanceChange: (String) -> Unit,
    onWalletCashChange: (String) -> Unit,
    onSaveStartingPointClicked: () -> Unit,
    onCheckClicked: () -> Unit,
    onSaveCheckClicked: () -> Unit,
    onAddGapAsIncomeClicked: () -> Unit,
    onIgnoreGapClicked: () -> Unit,
) {
    val lastCheck = uiState.lastCheck
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap),
    ) {
        Text(
            text = stringResource(R.string.balance_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (lastCheck == null) {
            StandardCard(tile = true) {
                Text(text = stringResource(R.string.balance_first_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.balance_first_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LastCheckTile(lastCheck)
        }

        AmountField(
            value = uiState.bankBalanceInput,
            onValueChange = onBankBalanceChange,
            label = stringResource(R.string.balance_bank_label),
            enabled = !uiState.isSaving,
        )
        AmountField(
            value = uiState.walletCashInput,
            onValueChange = onWalletCashChange,
            label = stringResource(R.string.balance_wallet_label),
            enabled = !uiState.isSaving,
        )
        if (uiState.isInputInvalid) {
            // A validation error, so red is right here (UI spec, section 1).
            Text(
                text = stringResource(R.string.balance_invalid_amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val result = uiState.result
        when {
            lastCheck == null -> Button(
                onClick = onSaveStartingPointClicked,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.balance_save_starting_point)) }

            result == null -> Button(
                onClick = onCheckClicked,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.balance_check_button)) }

            else -> ResultCard(
                result = result,
                isSaving = uiState.isSaving,
                onSaveCheckClicked = onSaveCheckClicked,
                onAddGapAsIncomeClicked = onAddGapAsIncomeClicked,
                onIgnoreGapClicked = onIgnoreGapClicked,
            )
        }

        uiState.saveError?.let { error ->
            CalmMessage(stringResource(error.messageRes()))
        }
        if (uiState.isSaved) {
            CalmMessage(stringResource(R.string.balance_saved))
        }
    }
}

@Composable
private fun LastCheckTile(lastCheck: LastCheckInfo) {
    StandardCard(tile = true) {
        Text(
            text = stringResource(R.string.balance_last_check_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = lastCheck.total, style = MaterialTheme.typography.titleLarge)
        Text(
            text = lastCheck.date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text(stringResource(R.string.balance_currency_prefix)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

// The comparison and what happens next. Calm wording and colours: a gap is normal, not a mistake.
@Composable
private fun ResultCard(
    result: CheckResult,
    isSaving: Boolean,
    onSaveCheckClicked: () -> Unit,
    onAddGapAsIncomeClicked: () -> Unit,
    onIgnoreGapClicked: () -> Unit,
) {
    StandardCard {
        ResultRow(stringResource(R.string.balance_expected), result.expected)
        ResultRow(stringResource(R.string.balance_actual), result.actual)
        ResultRow(stringResource(R.string.balance_difference), result.difference)
        Spacer(modifier = Modifier.height(8.dp))

        val explanation = when (result.kind) {
            GapKind.MATCHES -> stringResource(R.string.balance_matches)
            GapKind.UNRECORDED_SPENDING -> stringResource(R.string.balance_unrecorded, result.difference)
            GapKind.EXTRA_MONEY -> stringResource(R.string.balance_extra, result.difference)
        }
        Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (result.kind == GapKind.EXTRA_MONEY) {
            Button(onClick = onAddGapAsIncomeClicked, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.balance_add_income))
            }
            TextButton(onClick = onIgnoreGapClicked, enabled = !isSaving) {
                Text(stringResource(R.string.balance_ignore))
            }
        } else {
            Button(onClick = onSaveCheckClicked, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.balance_save_check))
            }
        }
    }
}

// Same layout as the spec's legend rows: label left (bodyMedium), value right (labelLarge).
@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CalmMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckPreview() {
    KavinduFinanceTheme {
        BalanceCheckContent(
            uiState = BalanceCheckUiState(
                isLoading = false,
                lastCheck = LastCheckInfo(date = "14 Oct", total = "LKR 105,000"),
                bankBalanceInput = "98000",
                walletCashInput = "1500",
                result = CheckResult(
                    expected = "LKR 101,000",
                    actual = "LKR 99,500",
                    difference = "LKR 1,500",
                    kind = GapKind.UNRECORDED_SPENDING,
                ),
            ),
            modifier = Modifier,
            onBankBalanceChange = {},
            onWalletCashChange = {},
            onSaveStartingPointClicked = {},
            onCheckClicked = {},
            onSaveCheckClicked = {},
            onAddGapAsIncomeClicked = {},
            onIgnoreGapClicked = {},
        )
    }
}
