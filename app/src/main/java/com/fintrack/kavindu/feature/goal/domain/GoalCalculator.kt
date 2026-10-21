package com.fintrack.kavindu.feature.goal.domain

import java.time.LocalDate

/** Neutral goal states. None of these is ever shown in red (NFR-09). */
enum class GoalStatus { ON_TRACK, A_LITTLE_BEHIND, REACHED, DEADLINE_PASSED }

/**
 * Everything the goal maths needs. All money is Long cents (LKR 1,250.50 = 125050).
 *
 * @param savedSinceStartCents income minus expenses since the goal was created (can be negative)
 * @param savedThisCycleCents  income minus expenses in the current pay cycle (can be negative)
 */
data class GoalInput(
    val targetCents: Long,
    val startingSavingsCents: Long,
    val savedSinceStartCents: Long,
    val savedThisCycleCents: Long,
    val goalStartDate: LocalDate,
    val deadline: LocalDate,
    val today: LocalDate,
    val cycleStartDay: Int = 25,
)

data class GoalProgress(
    val savedSoFarCents: Long,
    val remainingCents: Long,
    /** Whole percent, 0..100. */
    val percent: Int,
    /** Cycles left until the deadline, counting the current one. 0 once the deadline has passed. */
    val cyclesLeft: Int,
    /** "Needed this cycle": what this cycle must save to finish on time. Fixed for the whole cycle. */
    val neededThisCycleCents: Long,
    /** "LKR X to go" for this cycle. Never negative. */
    val toGoThisCycleCents: Long,
    /** End of the cycle in which the goal is expected to be reached, or null if it cannot be estimated yet. */
    val projectedFinish: LocalDate?,
    val status: GoalStatus,
)

/**
 * Turns a savings goal (e.g. LKR 490,000 for a MacBook in 12 months) into numbers Kavindu can act on
 * every pay cycle (FR-37, FR-38). Plain Kotlin with no Android code, so it is unit tested.
 */
class GoalCalculator {

    fun calculate(input: GoalInput): GoalProgress {
        val startDay = input.cycleStartDay
        val savedSoFar = input.startingSavingsCents + input.savedSinceStartCents
        val remaining = (input.targetCents - savedSoFar).coerceAtLeast(0)
        val percent = if (input.targetCents <= 0) 100
        else ((savedSoFar.coerceAtLeast(0) * 100) / input.targetCents).coerceIn(0, 100).toInt()

        val cyclesLeft = CycleDates.cyclesBetweenInclusive(input.today, input.deadline, startDay)

        // What still had to be saved when this cycle began, spread over the cycles left.
        // Using the start-of-cycle figure keeps "Needed this cycle" steady while he saves during the cycle.
        val remainingAtCycleStart = (input.targetCents - (savedSoFar - input.savedThisCycleCents)).coerceAtLeast(0)
        val neededThisCycle = when {
            remaining == 0L -> 0L
            cyclesLeft <= 0 -> remaining
            else -> ceilDiv(remainingAtCycleStart, cyclesLeft.toLong())
        }
        val toGo = (neededThisCycle - input.savedThisCycleCents).coerceAtLeast(0)

        // Average saving per *completed* cycle since the goal started. The current cycle is still running,
        // so counting it would make every early month look "behind" (RC5).
        val cyclesSinceStart = CycleDates.cyclesBetweenInclusive(input.goalStartDate, input.today, startDay)
        val completedCycles = (cyclesSinceStart - 1).coerceAtLeast(0)
        val savedInCompletedCycles = input.savedSinceStartCents - input.savedThisCycleCents
        val averagePerCycle = if (completedCycles > 0) savedInCompletedCycles / completedCycles else 0L

        val currentCycleStart = CycleDates.cycleStart(input.today, startDay)
        val projectedFinish: LocalDate? = when {
            remaining == 0L -> input.today
            averagePerCycle <= 0L -> null
            else -> {
                val cyclesNeeded = ceilDiv(remaining, averagePerCycle)
                // Finishes at the end of the (cyclesNeeded)-th cycle counting the current one.
                currentCycleStart.plusMonths(cyclesNeeded).minusDays(1)
            }
        }

        val status = when {
            remaining == 0L -> GoalStatus.REACHED
            input.today.isAfter(input.deadline) -> GoalStatus.DEADLINE_PASSED
            completedCycles == 0 -> GoalStatus.ON_TRACK // no full cycle yet: nothing fair to judge
            averagePerCycle >= neededThisCycle -> GoalStatus.ON_TRACK
            else -> GoalStatus.A_LITTLE_BEHIND
        }

        return GoalProgress(
            savedSoFarCents = savedSoFar,
            remainingCents = remaining,
            percent = percent,
            cyclesLeft = cyclesLeft,
            neededThisCycleCents = neededThisCycle,
            toGoThisCycleCents = toGo,
            projectedFinish = projectedFinish,
            status = status,
        )
    }

    private fun ceilDiv(a: Long, b: Long): Long = -Math.floorDiv(-a, b)
}
