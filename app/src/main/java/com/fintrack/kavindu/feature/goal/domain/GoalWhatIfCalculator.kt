package com.fintrack.kavindu.feature.goal.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** What the "What if" card needs from the goal. All money is Long cents. */
data class WhatIfInput(
    val remainingCents: Long,
    val neededThisCycleCents: Long,
    /** Average saved per completed cycle; 0 or less when there is no full cycle yet or he spent more than he earned. */
    val averagePerCycleCents: Long,
    val today: LocalDate,
    val cycleStartDay: Int,
    val deadline: LocalDate,
)

/** Where the finish month falls compared with the deadline month. Never shown in red. */
enum class WhatIfTiming { BEFORE_DEADLINE, IN_DEADLINE_MONTH, AFTER_DEADLINE }

data class WhatIfResult(
    val finish: LocalDate,
    val timing: WhatIfTiming,
    /** Whole months between the finish month and the deadline month (0 for IN_DEADLINE_MONTH). */
    val months: Int,
)

/**
 * The goal "what if": "If I save LKR X each cycle, when do I reach the goal?" (E1).
 * Plain Kotlin with no Android code, so it is unit tested. It uses the same rule as the "Projected finish" card
 * (the current cycle counts as cycle 1), so the two cards always agree. Nothing here is saved anywhere.
 */
object GoalWhatIfCalculator {

    /** The slider moves in steps of LKR 1,000. */
    const val STEP_CENTS = 100_000L

    private const val SMALLEST_MAX_CENTS = 5_000_000L // the slider always reaches at least LKR 50,000
    private const val MAX_ROUNDING_CENTS = 1_000_000L // and its end is a round LKR 10,000

    /** Where the slider starts: his real pace if he has one, otherwise what this cycle needs. */
    fun startingAmountCents(input: WhatIfInput): Long =
        snap(if (input.averagePerCycleCents > 0) input.averagePerCycleCents else input.neededThisCycleCents)

    /** The slider's end: twice the bigger of his pace and this cycle's need, so there is room to explore. */
    fun sliderMaxCents(input: WhatIfInput): Long {
        val wanted = maxOf(2 * input.neededThisCycleCents, 2 * input.averagePerCycleCents, SMALLEST_MAX_CENTS)
        return ceilDiv(wanted, MAX_ROUNDING_CENTS) * MAX_ROUNDING_CENTS
    }

    /** Rounds to the nearest LKR 1,000. */
    fun snap(cents: Long): Long = ((cents.coerceAtLeast(0) + STEP_CENTS / 2) / STEP_CENTS) * STEP_CENTS

    /** When the goal is reached saving [perCycleCents] each cycle; null for 0 (it would never be reached). */
    fun finish(input: WhatIfInput, perCycleCents: Long): WhatIfResult? {
        if (perCycleCents <= 0) return null
        val finish = if (input.remainingCents <= 0) {
            input.today
        } else {
            val cyclesNeeded = ceilDiv(input.remainingCents, perCycleCents)
            CycleDates.cycleStart(input.today, input.cycleStartDay).plusMonths(cyclesNeeded).minusDays(1)
        }
        val monthsAfter = ChronoUnit.MONTHS.between(YearMonth.from(input.deadline), YearMonth.from(finish)).toInt()
        val timing = when {
            monthsAfter < 0 -> WhatIfTiming.BEFORE_DEADLINE
            monthsAfter == 0 -> WhatIfTiming.IN_DEADLINE_MONTH
            else -> WhatIfTiming.AFTER_DEADLINE
        }
        return WhatIfResult(finish = finish, timing = timing, months = kotlin.math.abs(monthsAfter))
    }

    private fun ceilDiv(a: Long, b: Long): Long = -Math.floorDiv(-a, b)
}
