package com.fintrack.kavindu.feature.goal.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalFormValidatorTest {

    private val today = LocalDate.of(2026, 10, 25)

    private fun input(
        name: String = "MacBook Pro M4",
        target: String = "490,000",
        starting: String = "11200",
        deadline: LocalDate = today.plusYears(1),
    ) = GoalFormInput(name, target, starting, deadline)

    @Test
    fun `the MacBook goal is valid`() {
        val v = GoalFormValidator.validate(input(), today)
        assertTrue(v.isValid)
        assertEquals(49_000_000L, v.targetCents)
        assertEquals(1_120_000L, v.startingSavingsCents)
        assertEquals("MacBook Pro M4", v.name)
    }

    @Test
    fun `name is trimmed and required`() {
        assertEquals("MacBook", GoalFormValidator.validate(input(name = "  MacBook  "), today).name)
        assertEquals(GoalFieldError.NAME_REQUIRED, GoalFormValidator.validate(input(name = "   "), today).nameError)
    }

    @Test
    fun `name longer than 100 characters is rejected, as in the security rules`() {
        val v = GoalFormValidator.validate(input(name = "a".repeat(101)), today)
        assertEquals(GoalFieldError.NAME_TOO_LONG, v.nameError)
        assertFalse(v.isValid)
    }

    @Test
    fun `target must be above zero`() {
        for (text in listOf("", "0", "abc", "-1", "1.234")) {
            assertEquals("target '$text'", GoalFieldError.TARGET_INVALID, GoalFormValidator.validate(input(target = text), today).targetError)
        }
    }

    @Test
    fun `empty starting savings means zero`() {
        val v = GoalFormValidator.validate(input(starting = ""), today)
        assertTrue(v.isValid)
        assertEquals(0L, v.startingSavingsCents)
    }

    @Test
    fun `invalid starting savings is rejected`() {
        assertEquals(GoalFieldError.STARTING_INVALID, GoalFormValidator.validate(input(starting = "abc"), today).startingError)
    }

    @Test
    fun `deadline must be after today`() {
        assertEquals(GoalFieldError.DEADLINE_TOO_EARLY, GoalFormValidator.validate(input(deadline = today), today).deadlineError)
        assertEquals(GoalFieldError.DEADLINE_TOO_EARLY, GoalFormValidator.validate(input(deadline = today.minusDays(3)), today).deadlineError)
        assertTrue(GoalFormValidator.validate(input(deadline = today.plusDays(1)), today).isValid)
    }
}
