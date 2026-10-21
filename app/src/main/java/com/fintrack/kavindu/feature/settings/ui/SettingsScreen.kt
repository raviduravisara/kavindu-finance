package com.fintrack.kavindu.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.theme.KavinduFinanceTheme
import com.fintrack.kavindu.core.util.PayCycle
import com.fintrack.kavindu.feature.settings.viewmodel.SettingsMessage
import com.fintrack.kavindu.feature.settings.viewmodel.SettingsUiState
import com.fintrack.kavindu.feature.settings.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit,
    onBalanceCheckClicked: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.isLoading) {
        LoadingState()
    } else {
        SettingsContent(
            uiState = uiState,
            onBackClicked = onBackClicked,
            onBalanceCheckClicked = onBalanceCheckClicked,
            onStartDaySelected = viewModel::onStartDaySelected,
            onLogoutClicked = viewModel::onLogoutClicked,
            onLogoutConfirmed = viewModel::onLogoutConfirmed,
            onLogoutDismissed = viewModel::onLogoutDismissed,
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBackClicked: () -> Unit,
    onBalanceCheckClicked: () -> Unit,
    onStartDaySelected: (Int) -> Unit,
    onLogoutClicked: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    onLogoutDismissed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBackClicked) { Text(stringResource(R.string.action_back)) }
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        SectionTitle(stringResource(R.string.settings_account))
        Text(
            text = stringResource(R.string.auth_email),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = uiState.email, style = MaterialTheme.typography.bodyLarge)

        SectionTitle(stringResource(R.string.settings_pay_cycle))
        Text(text = stringResource(R.string.settings_start_day_label), style = MaterialTheme.typography.bodyLarge)
        StartDayPicker(selectedDay = uiState.cycleStartDay, onDaySelected = onStartDaySelected)
        Text(
            text = stringResource(R.string.settings_current_cycle, uiState.cycleStart, uiState.cycleEnd),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.settings_start_day_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (uiState.message) {
            SettingsMessage.SAVED -> Text(
                text = stringResource(R.string.settings_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SettingsMessage.SAVE_FAILED -> Text(
                text = stringResource(R.string.settings_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            null -> Unit
        }

        SectionTitle(stringResource(R.string.settings_balance_section))
        OutlinedButton(onClick = onBalanceCheckClicked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_balance_button))
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onLogoutClicked, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_log_out))
        }
    }

    if (uiState.isLogoutDialogVisible) {
        AlertDialog(
            onDismissRequest = onLogoutDismissed,
            title = { Text(stringResource(R.string.settings_log_out_title)) },
            text = { Text(stringResource(R.string.settings_log_out_text)) },
            confirmButton = {
                TextButton(onClick = onLogoutConfirmed) { Text(stringResource(R.string.action_log_out)) }
            },
            dismissButton = {
                TextButton(onClick = onLogoutDismissed) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

// Only days 1-28 can be picked, so every month has the start day.
@Composable
private fun StartDayPicker(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { isMenuOpen = true }) {
            Text(stringResource(R.string.settings_start_day_value, selectedDay))
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            for (day in PayCycle.MIN_START_DAY..PayCycle.MAX_START_DAY) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_start_day_value, day)) },
                    onClick = {
                        isMenuOpen = false
                        onDaySelected(day)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    KavinduFinanceTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isLoading = false,
                email = "name@example.com",
                cycleStart = "25 Sep",
                cycleEnd = "24 Oct",
            ),
            onBackClicked = {},
            onBalanceCheckClicked = {},
            onStartDaySelected = {},
            onLogoutClicked = {},
            onLogoutConfirmed = {},
            onLogoutDismissed = {},
        )
    }
}
