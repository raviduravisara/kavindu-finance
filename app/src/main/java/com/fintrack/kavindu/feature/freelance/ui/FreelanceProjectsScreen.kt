package com.fintrack.kavindu.feature.freelance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceFieldError
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceProjectsUiState
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceProjectsViewModel
import com.fintrack.kavindu.feature.freelance.viewmodel.ProjectRow
import com.fintrack.kavindu.core.ui.components.messageRes as appErrorMessageRes

/** Freelance projects (S1): what each client still owes, with overdue invoices flagged calmly (FR-11, FR-13). */
@Composable
fun FreelanceProjectsScreen(
    onBackClicked: () -> Unit,
    onProjectClicked: (String) -> Unit,
    viewModel: FreelanceProjectsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.freelance_title), onBackClicked = onBackClicked) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddProjectClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.freelance_add_project)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
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
            uiState.rows.isEmpty() -> EmptyState(
                message = stringResource(R.string.freelance_empty),
                actionLabel = stringResource(R.string.freelance_add_project),
                onAction = viewModel::onAddProjectClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> ProjectsList(uiState, onProjectClicked, Modifier.padding(innerPadding))
        }
    }

    if (uiState.isAddDialogVisible) {
        AddProjectDialog(
            uiState = uiState,
            onClientChange = viewModel::onClientChange,
            onProjectChange = viewModel::onProjectChange,
            onSave = viewModel::onSaveProjectClicked,
            onDismiss = viewModel::onAddProjectDismissed,
        )
    }
}

@Composable
private fun ProjectsList(uiState: FreelanceProjectsUiState, onProjectClicked: (String) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        item {
            HeroCard {
                Text(text = stringResource(R.string.freelance_still_owed), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = uiState.totalPending ?: stringResource(R.string.freelance_nothing_owed),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(text = stringResource(R.string.freelance_not_counted), style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(uiState.rows, key = { it.id }) { row ->
            ProjectCard(row = row, onClick = { onProjectClicked(row.id) })
        }
    }
}

// A project with overdue invoices uses the gentle prompt style, not red: a nudge to follow up, not an alarm.
@Composable
private fun ProjectCard(row: ProjectRow, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.projectName, style = MaterialTheme.typography.titleMedium)
                Text(text = row.clientName, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = row.pending ?: stringResource(R.string.freelance_paid_up), style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = stringResource(R.string.freelance_received_of, row.receivedCount, row.milestoneCount),
            style = MaterialTheme.typography.bodySmall,
        )
        if (row.overdueCount > 0) {
            Text(
                text = if (row.overdueCount == 1) stringResource(R.string.freelance_overdue_one)
                else stringResource(R.string.freelance_overdue_many, row.overdueCount),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    if (row.overdueCount > 0) {
        PromptCard(onClick = onClick) { content() }
    } else {
        StandardCard(tile = true, onClick = onClick) { content() }
    }
}

@Composable
private fun AddProjectDialog(
    uiState: FreelanceProjectsUiState,
    onClientChange: (String) -> Unit,
    onProjectChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.freelance_add_project)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap)) {
                DialogTextField(uiState.clientInput, onClientChange, stringResource(R.string.freelance_client_label), uiState.clientError, !uiState.isSaving)
                DialogTextField(uiState.projectInput, onProjectChange, stringResource(R.string.freelance_project_label), uiState.projectError, !uiState.isSaving)
                uiState.saveError?.let { Text(stringResource(it.appErrorMessageRes()), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !uiState.isSaving) { Text(stringResource(R.string.freelance_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.freelance_cancel)) } },
    )
}

@Composable
internal fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: FreelanceFieldError?,
    enabled: Boolean,
) {
    val errorText: String? = if (error != null) stringResource(error.messageRes()) else null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
