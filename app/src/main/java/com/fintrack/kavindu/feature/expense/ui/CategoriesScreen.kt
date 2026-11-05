package com.fintrack.kavindu.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.expense.viewmodel.CategoriesUiState
import com.fintrack.kavindu.feature.expense.viewmodel.CategoriesViewModel
import com.fintrack.kavindu.feature.expense.viewmodel.CategoryEditor
import com.fintrack.kavindu.feature.expense.viewmodel.CategoryRow

/**
 * Categories (M4): the committed and the discretionary ones, side by side in two groups, so Kavindu sees the split that
 * the dashboard is built on. Tap one to rename it or change its flag; add his own with the button.
 */
@Composable
fun CategoriesScreen(
    onBackClicked: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.categories_title), onBackClicked = onBackClicked) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.categories_add)) },
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
                message = stringResource(R.string.categories_empty),
                actionLabel = stringResource(R.string.categories_add),
                onAction = viewModel::onAddClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> CategoriesContent(uiState, viewModel::onCategoryClicked, Modifier.padding(innerPadding))
        }
    }

    uiState.editor?.let { editor ->
        CategoryEditorDialog(
            editor = editor,
            onNameChange = viewModel::onNameChange,
            onTypeSelected = viewModel::onTypeSelected,
            onSaveClicked = viewModel::onSaveClicked,
            onDismiss = viewModel::onEditorDismissed,
        )
    }
}

@Composable
private fun CategoriesContent(uiState: CategoriesUiState, onCategoryClicked: (String) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        if (uiState.committed.isNotEmpty()) {
            item { GroupHeader(R.string.categories_committed, R.string.categories_committed_help) }
            items(uiState.committed, key = { it.id }) { CategoryCard(it, onCategoryClicked) }
        }
        if (uiState.discretionary.isNotEmpty()) {
            item { GroupHeader(R.string.categories_discretionary, R.string.categories_discretionary_help) }
            items(uiState.discretionary, key = { it.id }) { CategoryCard(it, onCategoryClicked) }
        }
    }
}

@Composable
private fun GroupHeader(titleRes: Int, helpRes: Int) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(helpRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryCard(row: CategoryRow, onClick: (String) -> Unit) {
    StandardCard(tile = true, onClick = { onClick(row.id) }) {
        Text(text = row.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditor,
    onNameChange: (String) -> Unit,
    onTypeSelected: (CategoryType) -> Unit,
    onSaveClicked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val enabled = !editor.isSaving
    val nameError = editor.nameError
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editor.id == null) R.string.categories_add else R.string.categories_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.FormFieldGap)) {
                OutlinedTextField(
                    value = editor.nameInput,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.categories_name_label)) },
                    singleLine = true,
                    enabled = enabled,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(stringResource(it.errorRes())) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.categories_type_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // The flag Kavindu never has to decide per expense: committed (must pay) or discretionary (chose to spend).
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CategoryType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = type == editor.type,
                            onClick = { onTypeSelected(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = CategoryType.entries.size),
                            enabled = enabled,
                        ) {
                            Text(stringResource(type.labelRes()))
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.categories_flag_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                editor.saveError?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSaveClicked, enabled = enabled) { Text(stringResource(R.string.categories_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
