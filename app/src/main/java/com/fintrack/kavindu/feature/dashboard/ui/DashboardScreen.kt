package com.fintrack.kavindu.feature.dashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.HeroCard
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.PromptCard
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardDetails
import com.fintrack.kavindu.feature.dashboard.viewmodel.DashboardViewModel
import com.fintrack.kavindu.feature.dashboard.viewmodel.GoalSummary
import com.fintrack.kavindu.feature.expense.ui.QuickAddSheet
import com.fintrack.kavindu.feature.expense.ui.messageText
import com.fintrack.kavindu.feature.goal.domain.GoalStatus

/**
 * The home screen (M7), as in the "Dashboard" wireframe: one number to act on, where the money went, and the goal,
 * updating live. Quick-add is one tap away on the button, in a sheet over this screen (RC2).
 * Shows the ViewModel's state and nothing else: every number arrives already worked out.
 */
@Composable
fun DashboardScreen(
    onSettingsClicked: () -> Unit,
    onCheckInClicked: () -> Unit,
    onGoalClicked: () -> Unit,
    onOverdueInvoicesClicked: () -> Unit,
    onSubscriptionsClicked: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val details = uiState.details
    val snackbarHostState = remember { SnackbarHostState() }

    // After quick-add saves: a calm message, with the goal impact for a discretionary expense (wireframe 2, note 5).
    val savedText = uiState.savedOutcome?.messageText()
    LaunchedEffect(uiState.savedOutcome) {
        if (savedText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(savedText)
        viewModel.onSavedMessageShown()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.dashboard_title),
                subtitle = details?.let { cycleSubtitle(it) },
                appIcon = Icons.Filled.AccountBalanceWallet,
                actions = {
                    IconButton(onClick = onSettingsClicked) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onAddExpenseClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.dashboard_add_expense)) },
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
            details != null -> DashboardContent(
                details = details,
                onAddExpenseClicked = viewModel::onAddExpenseClicked,
                onCheckInClicked = onCheckInClicked,
                onGoalClicked = onGoalClicked,
                onOverdueInvoicesClicked = onOverdueInvoicesClicked,
                onSubscriptionsClicked = onSubscriptionsClicked,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (uiState.isQuickAddVisible) {
        QuickAddSheet(onDismiss = viewModel::onQuickAddDismissed, onSaved = viewModel::onExpenseSaved)
    }
}

// "25 Oct – 24 Nov · 12 days left", from the user's own pay cycle.
@Composable
private fun cycleSubtitle(details: DashboardDetails): String =
    if (details.daysLeft == 1) stringResource(R.string.dashboard_subtitle_one_day, details.cycleRange)
    else stringResource(R.string.dashboard_subtitle_days, details.cycleRange, details.daysLeft)

@Composable
private fun DashboardContent(
    details: DashboardDetails,
    onAddExpenseClicked: () -> Unit,
    onCheckInClicked: () -> Unit,
    onGoalClicked: () -> Unit,
    onOverdueInvoicesClicked: () -> Unit,
    onSubscriptionsClicked: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Extra bottom space so the last card is never hidden behind the Add expense button.
            .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        // 1. The number he can act on today.
        SafeToSpendCard(details)

        if (!details.hasEntries) {
            // A brand-new account: keep the hero and goal cards, and invite the first expense instead of showing empty charts.
            GoalCard(details.goal, onGoalClicked)
            FirstExpensePrompt(onAddExpenseClicked)
        } else {
            // 2. Received (never his estimate) and spent.
            Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StandardCard(tile = true, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.dashboard_received))
                    Text(text = details.received, style = MaterialTheme.typography.titleLarge)
                    details.pendingNotCounted?.let { pending ->
                        TileDetail(stringResource(R.string.dashboard_pending, pending))
                    }
                }
                StandardCard(tile = true, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.dashboard_spent))
                    Text(text = details.spent, style = MaterialTheme.typography.titleLarge)
                    TileDetail(stringResource(R.string.dashboard_since, details.cycleStart))
                }
            }

            // Gentle one-line nudges, only when there is something to look at.
            if (details.overdueInvoices > 0) {
                NudgeLine(
                    text = if (details.overdueInvoices == 1) stringResource(R.string.dashboard_overdue_one)
                    else stringResource(R.string.dashboard_overdue_many, details.overdueInvoices),
                    onClick = onOverdueInvoicesClicked,
                )
            }
            if (details.subscriptionsToReview > 0) {
                NudgeLine(
                    text = if (details.subscriptionsToReview == 1) stringResource(R.string.dashboard_subscriptions_one)
                    else stringResource(R.string.dashboard_subscriptions_many, details.subscriptionsToReview),
                    onClick = onSubscriptionsClicked,
                )
            }

            // 3. Two buckets, then the categories.
            WhereItWentCard(details)
            CategoryChartCard(details.categorySlices)

            // 4. The goal, every time he opens the app.
            GoalCard(details.goal, onGoalClicked)

            // 5. A weekly check-in instead of streaks: a gap is caught in one 30-second step.
            if (details.showCheckIn) CheckInPromptCard(onCheckInClicked)
        }
    }
}

// Overspending is shown as a plain negative number in the same colours, never as an alarm (NFR-09).
@Composable
private fun SafeToSpendCard(details: DashboardDetails) {
    HeroCard {
        Text(text = stringResource(R.string.dashboard_safe_label), style = MaterialTheme.typography.labelLarge)
        Text(text = details.safeToSpend, style = MaterialTheme.typography.displaySmall)
        Text(
            text = when {
                details.isOver -> stringResource(R.string.dashboard_over, details.overBy)
                details.daysLeft == 1 -> stringResource(R.string.dashboard_per_day_one, details.perDay)
                else -> stringResource(R.string.dashboard_per_day, details.perDay, details.daysLeft)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Tapping the card opens the Goal tab. Without a goal it points the way there.
@Composable
private fun GoalCard(goal: GoalSummary?, onClick: () -> Unit) {
    StandardCard(onClick = onClick) {
        if (goal == null) {
            Text(text = stringResource(R.string.goal_empty), style = MaterialTheme.typography.bodyMedium)
            return@StandardCard
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = goal.name, style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.dashboard_goal_percent, goal.percent), style = MaterialTheme.typography.labelLarge)
        }
        Text(text = goal.savedOfTarget, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { goal.progress },
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(8.dp),
        )
        TileDetail(goalStatusLine(goal))
    }
}

@Composable
private fun goalStatusLine(goal: GoalSummary): String = when (goal.status) {
    GoalStatus.REACHED -> stringResource(R.string.goal_status_reached)
    GoalStatus.DEADLINE_PASSED -> stringResource(R.string.goal_status_deadline_passed)
    GoalStatus.ON_TRACK, GoalStatus.A_LITTLE_BEHIND -> {
        val status = stringResource(
            if (goal.status == GoalStatus.ON_TRACK) R.string.goal_status_on_track else R.string.goal_status_behind
        )
        if (goal.isCycleSavingDone) stringResource(R.string.dashboard_goal_done, status)
        else stringResource(R.string.dashboard_goal_left, status, goal.toGoThisCycle)
    }
}

@Composable
private fun CheckInPromptCard(onStartClicked: () -> Unit) {
    PromptCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.checkin_prompt_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.checkin_prompt_text), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onStartClicked) { Text(stringResource(R.string.checkin_prompt_start)) }
        }
    }
}

@Composable
private fun FirstExpensePrompt(onAddExpenseClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dashboard_first_expense),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddExpenseClicked) { Text(stringResource(R.string.dashboard_add_expense)) }
    }
}

@Composable
private fun NudgeLine(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun TileLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TileDetail(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
