package com.fintrack.kavindu.feature.income.viewmodel

import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.util.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Why a field on the income form cannot be saved. Shown under that field (the one place red is allowed). */
enum class IncomeFieldError {
    AMOUNT_INVALID, // empty, zero, not a number, or more than 2 decimals
    LKR_RECEIVED_REQUIRED, // USD or USDT income without the LKR that reached the bank
    LKR_RECEIVED_INVALID,
    DATE_IN_FUTURE, // income is recorded when it arrives, never before
}

/** What the user typed. Text stays text until it is checked here. */
data class IncomeFormInput(
    val currency: Currency,
    val amountText: String,
    val lkrReceivedText: String,
    val date: LocalDate,
)

/** The checked result. When [isValid], the three amounts are ready to save. */
data class IncomeValidation(
    val amountCents: Long? = null,
    val lkrAmountCents: Long? = null,
    val rateCentsPerUnit: Long? = null,
    val amountError: IncomeFieldError? = null,
    val lkrReceivedError: IncomeFieldError? = null,
    val dateError: IncomeFieldError? = null,
) {
    val isValid: Boolean
        get() = amountError == null && lkrReceivedError == null && dateError == null &&
            amountCents != null && lkrAmountCents != null && rateCentsPerUnit != null
}

/**
 * Checks the Add income form (FR-08, FR-09). Plain Kotlin with no Android code, so it is unit tested.
 *
 * Multi-currency rule (design decision 4.3): for USD or USDT, Kavindu enters the original amount AND the
 * LKR that actually reached his bank. The rate is worked out from those two numbers once, stored with the
 * entry, and never recalculated, so a newer exchange rate never changes past income. No exchange-rate API.
 */
object IncomeInputValidator {

    /** LKR 1.00, the rate stored for LKR income (the security rules require exactly this). */
    const val LKR_RATE_CENTS = 100L

    fun validate(input: IncomeFormInput, today: LocalDate): IncomeValidation {
        val amount = Money.parseToCents(input.amountText)?.takeIf { it > 0 }
        val amountError = if (amount == null) IncomeFieldError.AMOUNT_INVALID else null
        val dateError = if (input.date.isAfter(today)) IncomeFieldError.DATE_IN_FUTURE else null

        if (input.currency == Currency.LKR) {
            return IncomeValidation(
                amountCents = amount,
                lkrAmountCents = amount,
                rateCentsPerUnit = LKR_RATE_CENTS,
                amountError = amountError,
                dateError = dateError,
            )
        }

        val lkrText = input.lkrReceivedText.trim()
        val lkr = Money.parseToCents(lkrText)?.takeIf { it > 0 }
        val lkrError = when {
            lkrText.isEmpty() -> IncomeFieldError.LKR_RECEIVED_REQUIRED
            lkr == null -> IncomeFieldError.LKR_RECEIVED_INVALID
            else -> null
        }
        val rate = if (amount != null && lkr != null) rateCentsPerUnit(amount, lkr) else null
        return IncomeValidation(
            amountCents = amount,
            lkrAmountCents = lkr,
            rateCentsPerUnit = rate,
            amountError = amountError,
            lkrReceivedError = lkrError,
            dateError = dateError,
        )
    }

    /**
     * LKR cents for 1 unit of the foreign currency, rounded to the nearest cent and at least 1.
     * Example: USD 50.00 (5,000 cents) became LKR 15,000.00 (1,500,000 cents) → 30,000 = LKR 300.00 per USD.
     */
    fun rateCentsPerUnit(amountCents: Long, lkrAmountCents: Long): Long {
        require(amountCents > 0) { "Amount must be positive" }
        return BigDecimal.valueOf(lkrAmountCents)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(amountCents), 0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(1)
    }
}

/** Formatting for income amounts in any currency, in the same style as Money.formatLkr. */
object IncomeFormat {

    /** "USD 50", "USDT 120.50", "LKR 15,000". */
    fun amount(cents: Long, currency: Currency): String =
        Money.formatLkr(cents).replaceFirst("LKR", currency.name)

    /** Shows a stored amount back in a form field without commas: 1500000 → "15000", 125050 → "1250.50". */
    fun toInputText(cents: Long): String {
        val whole = cents / 100
        val fraction = cents % 100
        return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
    }
}
