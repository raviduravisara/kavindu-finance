package com.fintrack.kavindu.feature.income.ui

import androidx.annotation.StringRes
import com.fintrack.kavindu.R
import com.fintrack.kavindu.core.model.IncomeSource
import com.fintrack.kavindu.feature.income.viewmodel.IncomeFieldError

// Text for the income enums. Kept in the UI layer because it only picks a string resource.

@StringRes
internal fun IncomeSource.labelRes(): Int = when (this) {
    IncomeSource.SALARY -> R.string.income_source_salary
    IncomeSource.FREELANCE -> R.string.income_source_freelance
    IncomeSource.ADSENSE -> R.string.income_source_adsense
    IncomeSource.OTHER -> R.string.income_source_other
}

@StringRes
internal fun IncomeFieldError.messageRes(): Int = when (this) {
    IncomeFieldError.AMOUNT_INVALID -> R.string.income_error_amount
    IncomeFieldError.LKR_RECEIVED_REQUIRED -> R.string.income_error_lkr_required
    IncomeFieldError.LKR_RECEIVED_INVALID -> R.string.income_error_lkr_invalid
    IncomeFieldError.DATE_IN_FUTURE -> R.string.income_error_date_future
}
