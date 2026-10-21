package com.fintrack.kavindu.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.PromptCard
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.theme.Dimens
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
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.settings_title), onBackClicked = onBackClicked) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
        } else {
            SettingsContent(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                onBalanceCheckClicked = onBalanceCheckClicked,
                onStartDaySelected = viewModel::onStartDaySelected,
                onLogoutClicked = viewModel::onLogoutClicked,
                onLogoutConfirmed = viewModel::onLogoutConfirmed,
                onLogoutDismissed = viewModel::onLogoutDismissed,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    modifier: Modifier,
    onBalanceCheckClicked: () -> Unit,
    onStartDaySelected: (Int) -> Unit,
    onLogoutClicked: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    onLogoutDismissed: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        StandardCard {
            Text(text = stringResource(R.string.settings_account), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.auth_email),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = uiState.email, style = MaterialTheme.typography.bodyLarge)
        }

        StandardCard {
            Text(text = stringResource(R.string.settings_pay_cycle), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            StartDayField(selectedDay = uiState.cycleStartDay, onDaySelected = onStartDaySelected)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_current_cycle, uiState.cycleStart, uiState.cycleEnd),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.settings_start_day_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val message = when (uiState.message) {
                SettingsMessage.SAVED -> stringResource(R.string.settings_saved)
                SettingsMessage.SAVE_FAILED -> stringResource(R.string.settings_save_failed)
                null -> null
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Same prompt card as the dashboard's weekly check-in (UI spec, section 2).
        PromptCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.checkin_prompt_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.checkin_prompt_text), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onBalanceCheckClicked) { Text(stringResource(R.string.checkin_prompt_start)) }
            }
        }

        TextButton(onClick = onLogoutClicked) { Text(stringResource(R.string.action_log_out)) }
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

// A read-only form field with a menu of days 1-28, so every month has the start day.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDayField(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = isMenuOpen, onExpandedChange = { isMenuOpen = it }) {
        OutlinedTextField(
            value = stringResource(R.string.settings_start_day_value, selectedDay),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_start_day_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuOpen) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
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
            modifier = Modifier,
            onBalanceCheckClicked = {},
            onStartDaySelected = {},
            onLogoutClicked = {},
            onLogoutConfirmed = {},
            onLogoutDismissed = {},
        )
    }
}
