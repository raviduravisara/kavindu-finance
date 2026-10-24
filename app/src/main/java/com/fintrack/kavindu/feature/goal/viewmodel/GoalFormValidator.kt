package com.fintrack.kavindu.feature.goal.viewmodel

import com.fintrack.kavindu.core.util.Money
import java.time.LocalDate

/** Why a field on the goal form cannot be saved. Shown under that field (the one place red is allowed). */
enum class GoalFieldError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    TARGET_INVALID, // empty, zero, not a number, or more than 2 decimals
    STARTING_INVALID, // not a number or more than 2 decimals (empty means 0)
    DEADLINE_TOO_EARLY, // the deadline must be after today
}

data class GoalFormInput(
    val name: String,
    val targetText: String,
    val startingSavingsText: String,
    val deadline: LocalDate,
)

data class GoalValidation(
    val name: String = "",
    val targetCents: Long? = null,
    val startingSavingsCents: Long? = null,
    val nameError: GoalFieldError? = null,
    val targetError: GoalFieldError? = null,
    val startingError: GoalFieldError? = null,
    val deadlineError: GoalFieldError? = null,
) {
    val isValid: Boolean
        get() = nameError == null && targetError == null && startingError == null && deadlineError == null &&
            targetCents != null && startingSavingsCents != null
}

/**
 * Checks the Set goal form (FR-36). Plain Kotlin, unit tested. The limits match the security rules:
 * a name of 1-100 characters, a target above 0, starting savings of 0 or more, and a deadline after the start.
 */
object GoalFormValidator {

    const val MAX_NAME_LENGTH = 100

    fun validate(input: GoalFormInput, today: LocalDate): GoalValidation {
        val name = input.name.trim()
        val nameError = when {
            name.isEmpty() -> GoalFieldError.NAME_REQUIRED
            name.length > MAX_NAME_LENGTH -> GoalFieldError.NAME_TOO_LONG
            else -> null
        }

        val target = Money.parseToCents(input.targetText)?.takeIf { it > 0 }
        val targetError = if (target == null) GoalFieldError.TARGET_INVALID else null

        // Starting savings is optional: empty means he starts from LKR 0.
        val startingText = input.startingSavingsText.trim()
        val starting = if (startingText.isEmpty()) 0L else Money.parseToCents(startingText)
        val startingError = if (starting == null) GoalFieldError.STARTING_INVALID else null

        val deadlineError = if (!input.deadline.isAfter(today)) GoalFieldError.DEADLINE_TOO_EARLY else null

        return GoalValidation(
            name = name,
            targetCents = target,
            startingSavingsCents = starting,
            nameError = nameError,
            targetError = targetError,
            startingError = startingError,
            deadlineError = deadlineError,
        )
    }
}
