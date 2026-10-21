package com.fintrack.kavindu.core.ui.components

import androidx.annotation.StringRes
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.AppError

// The friendly message for each app error. Use it with ErrorState, under a form, or in a snackbar:
// ErrorState(message = stringResource(error.messageRes()), onRetry = ...)
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.NO_NETWORK -> R.string.error_no_network
    AppError.NOT_SIGNED_IN -> R.string.error_not_signed_in
    AppError.SAVE_REJECTED -> R.string.error_save_rejected
    AppError.INVALID_DATA -> R.string.error_invalid_data
    AppError.SYNC_FAILED -> R.string.error_sync_failed
    AppError.UNKNOWN -> R.string.error_unknown
}
