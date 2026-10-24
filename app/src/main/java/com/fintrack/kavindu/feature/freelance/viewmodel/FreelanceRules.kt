package com.fintrack.kavindu.feature.freelance.viewmodel

import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.core.util.Money
import java.time.LocalDate

/** Why a freelance form field cannot be saved. Shown under that field. */
enum class FreelanceFieldError {
    CLIENT_REQUIRED,
    PROJECT_REQUIRED,
    TITLE_REQUIRED,
    TOO_LONG, // the security rules allow up to 100 characters
    AMOUNT_INVALID,
}

data class ProjectValidation(
    val clientName: String,
    val projectName: String,
    val clientError: FreelanceFieldError?,
    val projectError: FreelanceFieldError?,
) {
    val isValid: Boolean get() = clientError == null && projectError == null
}

data class MilestoneValidation(
    val title: String,
    val amountCents: Long?,
    val titleError: FreelanceFieldError?,
    val amountError: FreelanceFieldError?,
) {
    val isValid: Boolean get() = titleError == null && amountError == null && amountCents != null
}

/**
 * The freelance rules (S1, FR-11 to FR-14). Plain Kotlin with no Android code, so it is unit tested.
 *
 * Money owed is not money received: a PENDING milestone is never counted as income. It only becomes income
 * when Kavindu marks it received, which creates one linked income entry (FR-12, FR-14).
 */
object FreelanceRules {

    const val MAX_NAME_LENGTH = 100

    fun validateProject(clientName: String, projectName: String): ProjectValidation {
        val client = clientName.trim()
        val project = projectName.trim()
        return ProjectValidation(
            clientName = client,
            projectName = project,
            clientError = nameError(client, FreelanceFieldError.CLIENT_REQUIRED),
            projectError = nameError(project, FreelanceFieldError.PROJECT_REQUIRED),
        )
    }

    fun validateMilestone(title: String, amountText: String): MilestoneValidation {
        val cleanTitle = title.trim()
        val amount = Money.parseToCents(amountText)?.takeIf { it > 0 }
        return MilestoneValidation(
            title = cleanTitle,
            amountCents = amount,
            titleError = nameError(cleanTitle, FreelanceFieldError.TITLE_REQUIRED),
            amountError = if (amount == null) FreelanceFieldError.AMOUNT_INVALID else null,
        )
    }

    /** Overdue = still PENDING after its due date (the invoices he forgot to follow up, FR-13). */
    fun isOverdue(milestone: Milestone, today: LocalDate): Boolean =
        milestone.status == MilestoneStatus.PENDING && milestone.dueDate.isBefore(today)

    /**
     * What is still owed, per currency, e.g. [LKR 85,000, USD 200]. Currencies are kept apart because a pending
     * USD invoice has no LKR value until it is paid and the real rate is known.
     */
    fun pendingByCurrency(milestones: List<Milestone>): List<Pair<Currency, Long>> =
        Currency.entries.mapNotNull { currency ->
            val total = milestones
                .filter { it.status == MilestoneStatus.PENDING && it.currency == currency }
                .sumOf { it.amountCents }
            if (total > 0) currency to total else null
        }

    /** "LKR 85,000 + USD 200", or null when nothing is pending. */
    fun formatPending(pending: List<Pair<Currency, Long>>): String? =
        if (pending.isEmpty()) null
        else pending.joinToString(" + ") { (currency, cents) -> Money.formatLkr(cents).replaceFirst("LKR", currency.name) }

    private fun nameError(text: String, emptyError: FreelanceFieldError): FreelanceFieldError? = when {
        text.isEmpty() -> emptyError
        text.length > MAX_NAME_LENGTH -> FreelanceFieldError.TOO_LONG
        else -> null
    }
}
