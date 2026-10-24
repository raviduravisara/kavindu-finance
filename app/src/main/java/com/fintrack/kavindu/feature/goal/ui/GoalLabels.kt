package com.fintrack.kavindu.feature.goal.ui

import androidx.annotation.StringRes
import com.fintrack.kavindu.R
import com.fintrack.kavindu.feature.goal.domain.GoalStatus
import com.fintrack.kavindu.feature.goal.viewmodel.GoalFieldError

// Text for the goal enums. Calm words only: no red, no blame (NFR-09).

@StringRes
internal fun GoalStatus.labelRes(): Int = when (this) {
    GoalStatus.ON_TRACK -> R.string.goal_status_on_track
    GoalStatus.A_LITTLE_BEHIND -> R.string.goal_status_behind
    GoalStatus.REACHED -> R.string.goal_status_reached
    GoalStatus.DEADLINE_PASSED -> R.string.goal_status_deadline_passed
}

@StringRes
internal fun GoalFieldError.messageRes(): Int = when (this) {
    GoalFieldError.NAME_REQUIRED -> R.string.goal_error_name_required
    GoalFieldError.NAME_TOO_LONG -> R.string.goal_error_name_too_long
    GoalFieldError.TARGET_INVALID -> R.string.goal_error_target
    GoalFieldError.STARTING_INVALID -> R.string.goal_error_starting
    GoalFieldError.DEADLINE_TOO_EARLY -> R.string.goal_error_deadline
}
