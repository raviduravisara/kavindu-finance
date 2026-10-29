package com.fintrack.kavindu.feature.goal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.ui.components.AppTopBar
import com.fintrack.kavindu.core.ui.components.EmptyState
import com.fintrack.kavindu.core.ui.components.ErrorState
import com.fintrack.kavindu.core.ui.components.HeroCard
import com.fintrack.kavindu.core.ui.components.LoadingState
import com.fintrack.kavindu.core.ui.components.StandardCard
import com.fintrack.kavindu.core.ui.components.messageRes
import com.fintrack.kavindu.core.ui.theme.Dimens
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import com.fintrack.kavindu.feature.goal.viewmodel.EffectRow
import com.fintrack.kavindu.feature.goal.viewmodel.FinishTiming
import com.fintrack.kavindu.feature.goal.viewmodel.GoalDetails
import com.fintrack.kavindu.feature.goal.viewmodel.GoalViewModel

/**
 * The Goal tab (M6), as in the "Savings goal" wireframe: saved so far, this cycle's target,
 * the projected finish and what changed the date. Shows the ViewModel's state and nothing else.
 */
@Composable
fun GoalScreen(
    onEditGoalClicked: () -> Unit,
    onSetNewGoalClicked: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val details = uiState.details
    Scaffold(
        topBar = {
            AppTopBar(
                title = details?.name ?: stringResource(R.string.goal_tab_title),
                actions = {
                    if (details != null) {
                        IconButton(onClick = onEditGoalClicked) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = stringResource(R.string.goal_edit_title))
                        }
                    }
                },
            )
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
            details == null -> EmptyState(
                message = stringResource(R.string.goal_empty),
                actionLabel = stringResource(R.string.goal_set_title),
                onAction = onEditGoalClicked,
                modifier = Modifier.padding(innerPadding),
            )
            else -> GoalContent(details, onEditGoalClicked, onSetNewGoalClicked, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun GoalContent(
    details: GoalDetails,
    onEditGoalClicked: () -> Unit,
    onSetNewGoalClicked: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
    ) {
        when (details.status) {
            GoalStatus.REACHED -> ReachedCard(details, onSetNewGoalClicked)
            GoalStatus.DEADLINE_PASSED -> DeadlinePassedCard(details, onEditGoalClicked)
            else -> {
                SavedSoFarCard(details)
                CycleTiles(details)
                ProjectedFinishCard(details)
            }
        }
        EffectsCard(details.effects)
    }
}

// Pin 1: real numbers, not a feeling.
@Composable
private fun SavedSoFarCard(details: GoalDetails) {
    HeroCard {
        Text(text = stringResource(R.string.goal_saved_so_far), style = MaterialTheme.typography.labelLarge)
        Text(text = details.savedSoFar, style = MaterialTheme.typography.displaySmall)
        Text(
            text = stringResource(R.string.goal_of_target, details.target, details.percent, details.deadline),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        GoalProgressBar(details.progress)
    }
}

@Composable
private fun GoalProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        strokeCap = StrokeCap.Round,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    )
}

// Pin 2: the goal per pay cycle. Pin 5: a neutral status word.
@Composable
private fun CycleTiles(details: GoalDetails) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
        StandardCard(tile = true, modifier = Modifier.weight(1f)) {
            TileLabel(stringResource(R.string.goal_needed_this_cycle))
            Text(text = details.neededThisCycle, style = MaterialTheme.typography.titleLarge)
            TileDetail(details.cycleLabel)
        }
        StandardCard(tile = true, modifier = Modifier.weight(1f)) {
            TileLabel(stringResource(R.string.goal_saved_this_cycle))
            Text(text = details.savedThisCycle, style = MaterialTheme.typography.titleLarge)
            TileDetail(
                if (details.isCycleSavingDone) stringResource(R.string.goal_cycle_done)
                else stringResource(R.string.goal_to_go, details.toGoThisCycle),
            )
            TileDetail(stringResource(details.status.labelRes()))
        }
    }
}

// Pin 3: when he will finish at his current pace, without doing maths.
@Composable
private fun ProjectedFinishCard(details: GoalDetails) {
    val month = details.finishMonth
    val text = when {
        details.finishTiming == FinishTiming.NOT_YET_KNOWN || month == null -> stringResource(R.string.goal_finish_unknown)
        details.finishTiming == FinishTiming.AFTER_DEADLINE && details.monthsAfterDeadline == 1 ->
            stringResource(R.string.goal_finish_one_month_late, month)
        details.finishTiming == FinishTiming.AFTER_DEADLINE ->
            stringResource(R.string.goal_finish_months_late, month, details.monthsAfterDeadline)
        else -> stringResource(R.string.goal_finish_on_time, month)
    }
    StandardCard {
        Text(text = stringResource(R.string.goal_projected_finish), style = MaterialTheme.typography.titleMedium)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

// Pin 4: each purchase or check-in shows the days it added, linking everyday choices to the goal.
@Composable
private fun EffectsCard(effects: List<EffectRow>) {
    StandardCard {
        Text(text = stringResource(R.string.goal_what_changed), style = MaterialTheme.typography.titleMedium)
        if (effects.isEmpty()) {
            Text(
                text = stringResource(R.string.goal_no_effects),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        effects.forEach { effect -> EffectLine(effect) }
    }
}

@Composable
private fun EffectLine(effect: EffectRow) {
    val title = when {
        effect.isCheckIn -> stringResource(R.string.goal_effect_check_in)
        effect.title != null -> effect.title
        else -> stringResource(R.string.goal_effect_expense)
    }
    val detail = if (effect.isCheckIn) stringResource(R.string.goal_effect_unrecorded, effect.amount, effect.date)
    else stringResource(R.string.goal_effect_detail, effect.amount, effect.date)
    val days = when (effect.days) {
        0 -> stringResource(R.string.goal_days_under_one)
        1 -> stringResource(R.string.goal_days_one)
        else -> stringResource(R.string.goal_days_many, effect.days)
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                TileDetail(detail)
            }
            Text(text = days, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ReachedCard(details: GoalDetails, onSetNewGoalClicked: () -> Unit) {
    HeroCard {
        Text(text = stringResource(R.string.goal_status_reached), style = MaterialTheme.typography.displaySmall)
        Text(text = stringResource(R.string.goal_reached_text, details.savedSoFar, details.target), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        GoalProgressBar(1f)
        Button(onClick = onSetNewGoalClicked, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.goal_set_new))
        }
    }
}

// Calm, not red: what is still missing and a way forward.
@Composable
private fun DeadlinePassedCard(details: GoalDetails, onChooseNewDateClicked: () -> Unit) {
    HeroCard {
        Text(text = stringResource(R.string.goal_short, details.shortfall), style = MaterialTheme.typography.displaySmall)
        Text(text = stringResource(R.string.goal_deadline_passed_text, details.deadline, details.savedSoFar), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        GoalProgressBar(details.progress)
        Button(onClick = onChooseNewDateClicked, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.goal_choose_new_date))
        }
    }
}

@Composable
private fun TileLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TileDetail(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
