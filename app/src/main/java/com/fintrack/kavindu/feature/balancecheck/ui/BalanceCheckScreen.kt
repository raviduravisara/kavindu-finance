package com.fintrack.kavindu.feature.balancecheck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.messageRes
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
    val loadError = uiState.loadError
    when {
        uiState.isLoading -> LoadingState()
        loadError != null -> ErrorState(
            message = stringResource(loadError.messageRes()),
            onRetry = viewModel::onRetryClicked,
        )
        else -> BalanceCheckContent(
            uiState = uiState,
            onBackClicked = onBackClicked,
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

@Composable
private fun BalanceCheckContent(
    uiState: BalanceCheckUiState,
    onBackClicked: () -> Unit,
    onBankBalanceChange: (String) -> Unit,
    onWalletCashChange: (String) -> Unit,
    onSaveStartingPointClicked: () -> Unit,
    onCheckClicked: () -> Unit,
    onSaveCheckClicked: () -> Unit,
    onAddGapAsIncomeClicked: () -> Unit,
    onIgnoreGapClicked: () -> Unit,
) {
    val isFirstCheck = uiState.lastCheck == null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBackClicked) { Text(stringResource(R.string.action_back)) }
        Text(text = stringResource(R.string.balance_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.balance_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isFirstCheck) {
            Text(text = stringResource(R.string.balance_first_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.balance_first_text), style = MaterialTheme.typography.bodyMedium)
        } else {
            val last = uiState.lastCheck!!
            Text(
                text = stringResource(R.string.balance_last_check, last.date, last.total),
                style = MaterialTheme.typography.bodyMedium,
            )
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
            Text(
                text = stringResource(R.string.balance_invalid_amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val result = uiState.result
        when {
            isFirstCheck -> Button(
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
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (uiState.isSaved) {
            Text(
                text = stringResource(R.string.balance_saved),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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

// The comparison and what happens next. Calm wording: a gap is normal, not a mistake.
@Composable
private fun ResultCard(
    result: CheckResult,
    isSaving: Boolean,
    onSaveCheckClicked: () -> Unit,
    onAddGapAsIncomeClicked: () -> Unit,
    onIgnoreGapClicked: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultRow(stringResource(R.string.balance_expected), result.expected)
            ResultRow(stringResource(R.string.balance_actual), result.actual)
            ResultRow(stringResource(R.string.balance_difference), result.difference)

            val explanation = when (result.kind) {
                GapKind.MATCHES -> stringResource(R.string.balance_matches)
                GapKind.UNRECORDED_SPENDING -> stringResource(R.string.balance_unrecorded, result.difference)
                GapKind.EXTRA_MONEY -> stringResource(R.string.balance_extra, result.difference)
            }
            Text(text = explanation, style = MaterialTheme.typography.bodyMedium)

            if (result.kind == GapKind.EXTRA_MONEY) {
                Button(onClick = onAddGapAsIncomeClicked, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.balance_add_income))
                }
                OutlinedButton(onClick = onIgnoreGapClicked, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.balance_ignore))
                }
            } else {
                Button(onClick = onSaveCheckClicked, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.balance_save_check))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckPreview() {
    KavinduFinanceTheme {
        BalanceCheckContent(
            uiState = BalanceCheckUiState(
                isLoading = false,
                lastCheck = LastCheckInfo(date = "14 Oct", total = "LKR 105,000.00"),
                bankBalanceInput = "98000",
                walletCashInput = "1500",
                result = CheckResult(
                    expected = "LKR 101,000.00",
                    actual = "LKR 99,500.00",
                    difference = "LKR 1,500.00",
                    kind = GapKind.UNRECORDED_SPENDING,
                ),
            ),
            onBackClicked = {},
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
