package com.fintrack.kavindu.feature.goal.domain

import java.time.LocalDate

data class SafeToSpend(
    /** What is left to spend this cycle while staying on track. Negative means "LKR X over", shown calmly. */
    val amountCents: Long,
    /** Per remaining day, counting today. 0 when the amount is zero or negative. */
    val perDayCents: Long,
    val daysLeft: Int,
)

/**
 * The single number on the dashboard (FR-31):
 *
 *   safe to spend = income received this cycle − committed − goal saving needed this cycle − discretionary spent
 *
 * Only income already received counts; a pending freelance invoice counts as zero until paid (FR-14).
 * Example from the guide: 133,333 − 48,000 − 39,900 − 20,000 = LKR 25,433, about LKR 2,119 a day for 12 days.
 */
class SafeToSpendCalculator {

    fun calculate(
        receivedThisCycleCents: Long,
        committedThisCycleCents: Long,
        goalNeededThisCycleCents: Long,
        discretionarySpentCents: Long,
        today: LocalDate,
        cycleStartDay: Int = 25,
    ): SafeToSpend {
        val amount = receivedThisCycleCents -
            committedThisCycleCents -
            goalNeededThisCycleCents -
            discretionarySpentCents
        val daysLeft = CycleDates.daysLeftIncludingToday(today, cycleStartDay)
        val perDay = if (amount > 0) amount / daysLeft else 0L
        return SafeToSpend(amountCents = amount, perDayCents = perDay, daysLeft = daysLeft)
    }
}
