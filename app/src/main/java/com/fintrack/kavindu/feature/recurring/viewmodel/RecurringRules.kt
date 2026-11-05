package com.fintrack.kavindu.feature.recurring.viewmodel

import com.fintrack.kavindu.core.model.RecurringItem
import com.fintrack.kavindu.core.util.Money
import com.fintrack.kavindu.core.util.PayCycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Why a field on the recurring-item form cannot be saved. Shown under that field. */
enum class RecurringFieldError {
    NAME_REQUIRED,
    NAME_TOO_LONG, // the security rules allow 1 to 100 characters
    AMOUNT_INVALID,
    DAY_INVALID, // 1 to 28, so every month has the day
    CATEGORY_REQUIRED,
}

data class RecurringValidation(
    val name: String,
    val amountCents: Long?,
    val dayOfMonth: Int?,
    val nameError: RecurringFieldError?,
    val amountError: RecurringFieldError?,
    val dayError: RecurringFieldError?,
    val categoryError: RecurringFieldError?,
) {
    val isValid: Boolean
        get() = nameError == null && amountError == null && dayError == null && categoryError == null &&
            amountCents != null && dayOfMonth != null
}

/**
 * Rules for recurring items and the subscription review (M5, S6).
 * Plain Kotlin with no Android code, so it is unit tested.
 */
object RecurringRules {

    const val MAX_NAME_LENGTH = 100

    /**
     * A subscription is worth asking about again once this many days have passed since Kavindu last answered
     * "Still using this?" (or since he added it). He meant to cancel two subscriptions six months ago and
     * never did: a regular, gentle question is the fix.
     */
    const val REVIEW_AFTER_DAYS = 60L

    fun validate(name: String, amountText: String, dayText: String, categoryId: String?): RecurringValidation {
        val cleanName = name.trim()
        val cents = Money.parseToCents(amountText)?.takeIf { it > 0 }
        val day = dayText.trim().toIntOrNull()?.takeIf { PayCycle.isValidStartDay(it) }
        return RecurringValidation(
            name = cleanName,
            amountCents = cents,
            dayOfMonth = day,
            nameError = when {
                cleanName.isEmpty() -> RecurringFieldError.NAME_REQUIRED
                cleanName.length > MAX_NAME_LENGTH -> RecurringFieldError.NAME_TOO_LONG
                else -> null
            },
            amountError = if (cents == null) RecurringFieldError.AMOUNT_INVALID else null,
            dayError = if (day == null) RecurringFieldError.DAY_INVALID else null,
            categoryError = if (categoryId.isNullOrBlank()) RecurringFieldError.CATEGORY_REQUIRED else null,
        )
    }

    /** Whether the dashboard should gently suggest reviewing [item]: an active subscription not checked for a while. */
    fun needsReview(item: RecurringItem, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!item.isSubscription || !item.isActive) return false
        val lastChecked = item.lastReviewedDate ?: Instant.ofEpochMilli(item.createdAt).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(lastChecked, today) >= REVIEW_AFTER_DAYS
    }

    /** What his active subscriptions cost every month, in cents. */
    fun monthlySubscriptionCents(items: List<RecurringItem>): Long =
        items.filter { it.isSubscription && it.isActive }.sumOf { it.amountCents }
}
