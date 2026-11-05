package com.fintrack.kavindu.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.expense.viewmodel.QuickAddUiState
import com.fintrack.kavindu.feature.expense.viewmodel.QuickAddViewModel
import com.fintrack.kavindu.feature.expense.viewmodel.SaveOutcome
import kotlinx.coroutines.launch

// The keypad layout from the wireframe: 4 rows of 3 keys. '.' is the decimal point and '<' is backspace.
private val KEYPAD_ROWS = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('.', '0', '<'),
)

/**
 * The quick-add expense sheet (M3), as in the "Add expense" wireframe. It opens over the dashboard (or history)
 * with the keypad ready, so recording an expense is amount -> category -> Save: 3 taps (RC2, FR-18).
 * The amount is typed on this keypad, never on the system keyboard. Shows the ViewModel's state and nothing else.
 *
 * Show it only while it should be visible: every time it enters the composition it starts with a clean amount.
 * [onSaved] is called after the sheet has slid away, with what the screen needs for its message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSaved: (SaveOutcome) -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.onSheetOpened() }

    // Saved: take the outcome out of the state (so it is never delivered twice), slide the sheet away, then let the
    // screen close it and show its message. This runs in its own scope: clearing the state changes this effect's key,
    // which would cancel the effect itself halfway through the slide.
    LaunchedEffect(uiState.outcome) {
        val outcome = uiState.outcome ?: return@LaunchedEffect
        viewModel.onOutcomeConsumed()
        scope.launch {
            sheetState.hide()
            onSaved(outcome)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Dimens.SheetTopCorner, topEnd = Dimens.SheetTopCorner),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        QuickAddContent(
            uiState = uiState,
            onDateChipClicked = viewModel::onDateChipClicked,
            onCategorySelected = viewModel::onCategorySelected,
            onChannelSelected = viewModel::onChannelSelected,
            onAccountSelected = viewModel::onAccountSelected,
            onDigitPressed = viewModel::onDigitPressed,
            onDotPressed = viewModel::onDotPressed,
            onBackspacePressed = viewModel::onBackspacePressed,
            onSaveClicked = viewModel::onSaveClicked,
            onRetryClicked = viewModel::onRetryClicked,
        )
    }

    if (uiState.isDatePickerVisible) {
        ExpenseDatePicker(
            selectedMillis = uiState.dateMillis,
            maxMillis = uiState.todayMillis,
            onDateSelected = viewModel::onDateSelected,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }
}

@Composable
private fun QuickAddContent(
    uiState: QuickAddUiState,
    onDateChipClicked: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onChannelSelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDigitPressed: (Char) -> Unit,
    onDotPressed: () -> Unit,
    onBackspacePressed: () -> Unit,
    onSaveClicked: () -> Unit,
    onRetryClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Header: title, and the date chip (Today unless he changes it).
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.quickadd_title), style = MaterialTheme.typography.titleLarge)
            FilterChip(
                selected = false,
                onClick = onDateChipClicked,
                label = { Text(if (uiState.dateIsToday) stringResource(R.string.quickadd_date_today) else uiState.dateText) },
            )
        }

        // 2. Amount: typed on the keypad below, shown big, centred.
        AmountDisplay(display = uiState.amountDisplay)

        // 3. Category chips, most-used first, exactly one selected.
        CategorySection(uiState, onCategorySelected, onRetryClicked)

        // 4. Channel and account, filled in from last time.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
            Text(
                text = stringResource(R.string.quickadd_paid_with),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChoiceChip(selected = uiState.channel, options = uiState.channels, onSelected = onChannelSelected)
            ChoiceChip(selected = uiState.account, options = uiState.accounts, onSelected = onAccountSelected)
        }

        // 5. Keypad.
        Keypad(onDigitPressed, onDotPressed, onBackspacePressed)

        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 6. Save: enabled once there is an amount and a category.
        Button(onClick = onSaveClicked, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quickadd_save))
        }
    }
}

@Composable
private fun AmountDisplay(display: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.quickadd_amount_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = display, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(uiState: QuickAddUiState, onCategorySelected: (String) -> Unit, onRetryClicked: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
        Text(
            text = stringResource(R.string.quickadd_category_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val loadError = uiState.loadError
        when {
            loadError != null -> {
                Text(
                    text = stringResource(loadError.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetryClicked) { Text(stringResource(R.string.action_try_again)) }
            }
            uiState.isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
            uiState.categories.isEmpty() -> Text(
                text = stringResource(R.string.quickadd_no_categories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
            ) {
                uiState.categories.forEach { category ->
                    val selected = category.id == uiState.selectedCategoryId
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = if (selected) {
                            { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Keypad(onDigitPressed: (Char) -> Unit, onDotPressed: () -> Unit, onBackspacePressed: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
        KEYPAD_ROWS.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap)) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        onClick = {
                            when (key) {
                                '<' -> onBackspacePressed()
                                '.' -> onDotPressed()
                                else -> onDigitPressed(key)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(key: Char, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(Dimens.KeyHeight),
        shape = RoundedCornerShape(Dimens.KeyCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key == '<') {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.quickadd_backspace),
                )
            } else {
                Text(text = key.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
