package com.fintrack.kavindu.feature.recurring.viewmodel

import com.fintrack.kavindu.core.model.RecurringItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringRulesTest {

    private val utc = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 13)

    private fun sub(
        created: LocalDate = today.minusDays(200),
        reviewed: LocalDate? = null,
        active: Boolean = true,
        isSubscription: Boolean = true,
        amountCents: Long = 129_900,
    ) = RecurringItem(
        id = "netflix", name = "Netflix", amountCents = amountCents, categoryId = "subs", dayOfMonth = 5,
        isSubscription = isSubscription, isActive = active, lastReviewedDate = reviewed,
        createdAt = created.atStartOfDay(utc).toInstant().toEpochMilli(),
    )

    // ---- the form ----

    @Test
    fun `a good form is valid`() {
        val v = RecurringRules.validate(" Rent ", "34,000", "25", "cat")
        assertTrue(v.isValid)
        assertEquals("Rent", v.name)
        assertEquals(3_400_000L, v.amountCents)
        assertEquals(25, v.dayOfMonth)
    }

    @Test
    fun `every bad field has its own error`() {
        val v = RecurringRules.validate("", "abc", "31", null)
        assertEquals(RecurringFieldError.NAME_REQUIRED, v.nameError)
        assertEquals(RecurringFieldError.AMOUNT_INVALID, v.amountError)
        assertEquals(RecurringFieldError.DAY_INVALID, v.dayError)
        assertEquals(RecurringFieldError.CATEGORY_REQUIRED, v.categoryError)
        assertFalse(v.isValid)
    }

    @Test
    fun `the due day is limited to 1 to 28 like the pay cycle`() {
        assertNull(RecurringRules.validate("x", "1", "1", "c").dayError)
        assertNull(RecurringRules.validate("x", "1", "28", "c").dayError)
        assertEquals(RecurringFieldError.DAY_INVALID, RecurringRules.validate("x", "1", "0", "c").dayError)
        assertEquals(RecurringFieldError.DAY_INVALID, RecurringRules.validate("x", "1", "29", "c").dayError)
        assertEquals(RecurringFieldError.DAY_INVALID, RecurringRules.validate("x", "1", "", "c").dayError)
    }

    @Test
    fun `the name is limited to 100 characters`() {
        assertNull(RecurringRules.validate("x".repeat(100), "1", "1", "c").nameError)
        assertEquals(RecurringFieldError.NAME_TOO_LONG, RecurringRules.validate("x".repeat(101), "1", "1", "c").nameError)
    }

    @Test
    fun `an amount of zero is rejected`() {
        assertEquals(RecurringFieldError.AMOUNT_INVALID, RecurringRules.validate("x", "0", "1", "c").amountError)
    }

    // ---- subscription review ----

    @Test
    fun `a subscription never reviewed for over 60 days needs a review`() {
        assertTrue(RecurringRules.needsReview(sub(created = today.minusDays(200)), today, utc))
    }

    @Test
    fun `a subscription added a few days ago does not nag yet`() {
        assertFalse(RecurringRules.needsReview(sub(created = today.minusDays(3)), today, utc))
    }

    @Test
    fun `answering Still using this restarts the clock`() {
        assertFalse(RecurringRules.needsReview(sub(reviewed = today.minusDays(10)), today, utc))
        assertTrue(RecurringRules.needsReview(sub(reviewed = today.minusDays(60)), today, utc))
        assertFalse(RecurringRules.needsReview(sub(reviewed = today.minusDays(59)), today, utc))
    }

    @Test
    fun `a paused item or a plain bill is never reviewed`() {
        assertFalse(RecurringRules.needsReview(sub(active = false), today, utc))
        assertFalse(RecurringRules.needsReview(sub(isSubscription = false), today, utc))
    }

    @Test
    fun `monthly subscription cost counts only active subscriptions`() {
        val items = listOf(
            sub(amountCents = 129_900),
            sub(amountCents = 50_000),
            sub(amountCents = 999_900, active = false),
            sub(amountCents = 3_400_000, isSubscription = false),
        )
        assertEquals(179_900L, RecurringRules.monthlySubscriptionCents(items))
    }
}
