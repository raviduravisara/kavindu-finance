package com.fintrack.kavindu.feature.recurring.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringEditor
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringFieldError
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringRow
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringUiState
import com.fintrack.kavindu.feature.recurring.viewmodel.RecurringViewModel

@StringRes
private fun RecurringFieldError.errorRes(): Int = when (this) {
    RecurringFieldError.NAME_REQUIRED -> R.string.recurring_error_name_required
    RecurringFieldError.NAME_TOO_LONG -> R.string.recurring_error_name_too_long
    RecurringFieldError.AMOUNT_INVALID -> R.string.recurring_error_amount
    RecurringFieldError.DAY_INVALID -> R.string.recurring_error_day
    RecurringFieldError.CATEGORY_REQUIRED -> R.string.recurring_error_category
}

/**
 * Recurring items and the subscription review (M5, S6): rent, gym and subscriptions entered once and added by the app
 * every month. Each subscription asks "Still using this?" next to what it costs, because Kavindu meant to cancel two of
 * them six months ago. Shows the ViewModel's state and nothing else.
 */
@Composable
fun RecurringScreen(
    onBackClicked: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorText = uiState.actionError?.let { stringResource(it.messageRes()) }
    LaunchedEffect(uiState.actionError) {
        if (errorText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(errorText)
        viewModel.onActionErrorShown()
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.recurring_title), onBackClicked = onBackClicked) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.recurring_add)) },
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
                message = stringResource(R.string.recurring_empty),
                actionLabel = stringResource(R.string.recurring_add),
                onAction = viewModel::onAddClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> RecurringContent(
                uiState = uiState,
                onItemClicked = viewModel::onItemClicked,
                onStillUsingClicked = viewModel::onStillUsingClicked,
                onPauseClicked = viewModel::onPauseClicked,
                onResumeClicked = viewModel::onResumeClicked,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    uiState.editor?.let { editor ->
        RecurringEditorDialog(
            editor = editor,
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onDayChange = viewModel::onDayChange,
            onCategorySelected = viewModel::onCategorySelected,
            onEstimateChange = viewModel::onEstimateChange,
            onSubscriptionChange = viewModel::onSubscriptionChange,
            onActiveChange = viewModel::onActiveChange,
            onSaveClicked = viewModel::onSaveClicked,
            onDeleteClicked = viewModel::onDeleteClicked,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteDismissed = viewModel::onDeleteDismissed,
            onDismiss = viewModel::onEditorDismissed,
        )
    }
}

@Composable
private fun RecurringContent(
    uiState: RecurringUiState,
    onItemClicked: (String) -> Unit,
    onStillUsingClicked: (String) -> Unit,
    onPauseClicked: (String) -> Unit,
    onResumeClicked: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Extra bottom space so the last item is never hidden behind the Add button.
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        if (uiState.subscriptions.isNotEmpty()) {
            item { SubscriptionsHeader(uiState.monthlySubscriptions, uiState.yearlySubscriptions) }
            items(uiState.subscriptions, key = { "sub-${it.id}" }) { row ->
                SubscriptionCard(row, onItemClicked, onStillUsingClicked, onPauseClicked)
            }
        }
        if (uiState.bills.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.recurring_bills_title)) }
            items(uiState.bills, key = { "bill-${it.id}" }) { row -> ItemCard(row, onItemClicked, null) }
        }
        if (uiState.paused.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.recurring_paused_title)) }
            items(uiState.paused, key = { "paused-${it.id}" }) { row -> ItemCard(row, onItemClicked) { onResumeClicked(row.id) } }
        }
    }
}

// The real monthly cost beside the question, so deciding is easy.
@Composable
private fun SubscriptionsHeader(monthly: String?, yearly: String?) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        SectionTitle(stringResource(R.string.recurring_subscriptions_title))
        if (monthly != null && yearly != null) {
            Text(
                text = stringResource(R.string.recurring_subscriptions_cost, monthly, yearly),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.recurring_pause_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SubscriptionCard(
    row: RecurringRow,
    onItemClicked: (String) -> Unit,
    onStillUsingClicked: (String) -> Unit,
    onPauseClicked: (String) -> Unit,
) {
    StandardCard(onClick = { onItemClicked(row.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = row.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = row.amount, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = rowDetail(row),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = row.lastChecked?.let { stringResource(R.string.recurring_last_checked, it) }
                ?: stringResource(R.string.recurring_never_checked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.recurring_still_using), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { onStillUsingClicked(row.id) }) { Text(stringResource(R.string.recurring_yes)) }
            TextButton(onClick = { onPauseClicked(row.id) }) { Text(stringResource(R.string.recurring_pause)) }
        }
    }
}

@Composable
private fun ItemCard(row: RecurringRow, onItemClicked: (String) -> Unit, onResumeClicked: (() -> Unit)?) {
    StandardCard(tile = true, onClick = { onItemClicked(row.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = rowDetail(row),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = row.amount, style = MaterialTheme.typography.titleMedium)
                if (row.isEstimate) {
                    Text(
                        text = stringResource(R.string.recurring_estimate_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onResumeClicked != null) {
                TextButton(onClick = onResumeClicked) { Text(stringResource(R.string.recurring_resume)) }
            }
        }
    }
}

// e.g. "Rent · every month on day 25"
@Composable
private fun rowDetail(row: RecurringRow): String {
    val schedule = stringResource(R.string.recurring_row_detail_schedule, row.dueDay)
    return if (row.categoryName != null) stringResource(R.string.recurring_row_detail_category, row.categoryName, schedule) else schedule
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringEditorDialog(
    editor: RecurringEditor,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onEstimateChange: (Boolean) -> Unit,
    onSubscriptionChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val enabled = !editor.isSaving
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editor.id == null) R.string.recurring_add else R.string.recurring_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
            ) {
                FormField(
                    value = editor.nameInput,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.recurring_name_label),
                    placeholder = stringResource(R.string.recurring_name_placeholder),
                    error = editor.nameError,
                    enabled = enabled,
                )
                FormField(
                    value = editor.amountInput,
                    onValueChange = onAmountChange,
                    label = stringResource(R.string.recurring_amount_label),
                    prefix = "LKR ",
                    error = editor.amountError,
                    enabled = enabled,
                    keyboardType = KeyboardType.Decimal,
                )
                FormField(
                    value = editor.dayInput,
                    onValueChange = onDayChange,
                    label = stringResource(R.string.recurring_day_label),
                    error = editor.dayError,
                    enabled = enabled,
                    keyboardType = KeyboardType.Number,
                )

                Text(
                    text = stringResource(R.string.recurring_category_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ChipGap),
                ) {
                    editor.categories.forEach { category ->
                        FilterChip(
                            selected = category.id == editor.selectedCategoryId,
                            onClick = { onCategorySelected(category.id) },
                            label = { Text(category.name) },
                            enabled = enabled,
                        )
                    }
                }
                editor.categoryError?.let {
                    Text(text = stringResource(it.errorRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                SwitchRow(stringResource(R.string.recurring_estimate_switch), editor.isEstimate, enabled, onEstimateChange)
                if (editor.isEstimate) {
                    Text(
                        text = stringResource(R.string.recurring_estimate_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SwitchRow(stringResource(R.string.recurring_subscription_switch), editor.isSubscription, enabled, onSubscriptionChange)
                if (editor.id != null) {
                    SwitchRow(stringResource(R.string.recurring_active_switch), editor.isActive, enabled, onActiveChange)
                }

                editor.saveError?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (editor.id != null) {
                    TextButton(onClick = onDeleteClicked, enabled = enabled) { Text(stringResource(R.string.recurring_delete)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSaveClicked, enabled = enabled) { Text(stringResource(R.string.recurring_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (editor.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.recurring_delete_title)) },
            text = { Text(stringResource(R.string.recurring_delete_text)) },
            confirmButton = { TextButton(onClick = onDeleteConfirmed) { Text(stringResource(R.string.recurring_delete)) } },
            dismissButton = { TextButton(onClick = onDeleteDismissed) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: RecurringFieldError?,
    enabled: Boolean,
    placeholder: String? = null,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        isError = error != null, // validation errors are the one place red is allowed (UI spec, section 1)
        supportingText = error?.let { { Text(stringResource(it.errorRes())) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
