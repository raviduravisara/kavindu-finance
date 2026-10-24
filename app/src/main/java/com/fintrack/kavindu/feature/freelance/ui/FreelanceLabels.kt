package com.fintrack.kavindu.feature.freelance.ui

import androidx.annotation.StringRes
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.feature.freelance.viewmodel.FreelanceFieldError

// Text for the freelance enums.

@StringRes
internal fun MilestoneStatus.labelRes(): Int = when (this) {
    MilestoneStatus.PENDING -> R.string.freelance_status_pending
    MilestoneStatus.RECEIVED -> R.string.freelance_status_received
    MilestoneStatus.CANCELLED -> R.string.freelance_status_cancelled
}

@StringRes
internal fun FreelanceFieldError.messageRes(): Int = when (this) {
    FreelanceFieldError.CLIENT_REQUIRED -> R.string.freelance_error_client
    FreelanceFieldError.PROJECT_REQUIRED -> R.string.freelance_error_project
    FreelanceFieldError.TITLE_REQUIRED -> R.string.freelance_error_title
    FreelanceFieldError.TOO_LONG -> R.string.freelance_error_too_long
    FreelanceFieldError.AMOUNT_INVALID -> R.string.freelance_error_amount
}
