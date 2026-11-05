package com.fintrack.kavindu.feature.expense.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.feature.expense.viewmodel.CategoryNameError
import com.fintrack.kavindu.feature.expense.viewmodel.ExpenseFieldError
import com.fintrack.kavindu.feature.expense.viewmodel.SaveOutcome

// Small pieces shared by the quick-add sheet, the edit-expense form, history and categories.

@StringRes
internal fun CategoryType.labelRes(): Int = when (this) {
    CategoryType.COMMITTED -> R.string.history_committed
    CategoryType.DISCRETIONARY -> R.string.history_discretionary
}

@StringRes
internal fun ExpenseFieldError.errorRes(): Int = when (this) {
    ExpenseFieldError.AMOUNT_INVALID -> R.string.editexpense_error_amount
    ExpenseFieldError.CATEGORY_REQUIRED -> R.string.editexpense_error_category
    ExpenseFieldError.DATE_IN_FUTURE -> R.string.editexpense_error_date_future
    ExpenseFieldError.NOTE_TOO_LONG -> R.string.editexpense_error_note_too_long
}

@StringRes
internal fun CategoryNameError.errorRes(): Int = when (this) {
    CategoryNameError.REQUIRED -> R.string.categories_error_required
    CategoryNameError.TOO_LONG -> R.string.categories_error_too_long
    CategoryNameError.DUPLICATE -> R.string.categories_error_duplicate
}

/**
 * The calm message after saving an expense (S5, FR-32). A discretionary expense says how many days it moves the goal
 * date, in neutral words with no warning colour; anything else just says it is saved.
 */
@Composable
internal fun SaveOutcome.messageText(): String = when {
    !isDiscretionary || goalName == null || daysDelayed <= 0 -> stringResource(R.string.quickadd_saved)
    daysDelayed == 1 -> stringResource(R.string.quickadd_saved_goal_one, goalName)
    else -> stringResource(R.string.quickadd_saved_goal_many, goalName, daysDelayed)
}

/** The date picker used by quick-add and edit-expense. Future days are greyed out: an expense is recorded on or after the day it happened. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDatePicker(
    selectedMillis: Long,
    maxMillis: Long,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) { Text(stringResource(R.string.expense_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * A selected chip that shows the current choice (e.g. "Card") and opens a small menu to change it, as in the
 * "Paid with" row of the quick-add wireframe. [expanded] is only whether the menu is open, which is display state.
 */
@Composable
internal fun ChoiceChip(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(selected) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Check, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}
