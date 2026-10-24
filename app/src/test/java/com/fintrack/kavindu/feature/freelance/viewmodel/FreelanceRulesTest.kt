package com.fintrack.kavindu.feature.freelance.viewmodel

import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FreelanceRulesTest {

    private val today = LocalDate.of(2026, 10, 24)
    private fun lkr(rupees: Long) = rupees * 100

    private fun milestone(
        id: String,
        rupees: Long,
        due: LocalDate,
        status: MilestoneStatus = MilestoneStatus.PENDING,
        currency: Currency = Currency.LKR,
    ) = Milestone(
        id = id, projectId = "p1", title = "Milestone $id", amountCents = lkr(rupees), currency = currency,
        dueDate = due, status = status, incomeId = if (status == MilestoneStatus.RECEIVED) "inc-$id" else null, createdAt = 1,
    )

    @Test
    fun `a project needs a client and a project name`() {
        val v = FreelanceRules.validateProject("  Lanka Bakers ", " WordPress site ")
        assertTrue(v.isValid)
        assertEquals("Lanka Bakers", v.clientName)
        assertEquals("WordPress site", v.projectName)

        val empty = FreelanceRules.validateProject(" ", "")
        assertEquals(FreelanceFieldError.CLIENT_REQUIRED, empty.clientError)
        assertEquals(FreelanceFieldError.PROJECT_REQUIRED, empty.projectError)
    }

    @Test
    fun `names over 100 characters are rejected, as in the security rules`() {
        assertEquals(FreelanceFieldError.TOO_LONG, FreelanceRules.validateProject("a".repeat(101), "x").clientError)
    }

    @Test
    fun `a milestone needs a title and an amount above zero`() {
        val v = FreelanceRules.validateMilestone("Design approved", "45,000")
        assertTrue(v.isValid)
        assertEquals(lkr(45_000), v.amountCents)

        val bad = FreelanceRules.validateMilestone("", "0")
        assertFalse(bad.isValid)
        assertEquals(FreelanceFieldError.TITLE_REQUIRED, bad.titleError)
        assertEquals(FreelanceFieldError.AMOUNT_INVALID, bad.amountError)
    }

    @Test
    fun `only pending milestones past their due date are overdue`() {
        assertTrue(FreelanceRules.isOverdue(milestone("a", 1, today.minusDays(1)), today))
        assertFalse(FreelanceRules.isOverdue(milestone("b", 1, today), today)) // due today is not late yet
        assertFalse(FreelanceRules.isOverdue(milestone("c", 1, today.plusDays(5)), today))
        assertFalse(FreelanceRules.isOverdue(milestone("d", 1, today.minusDays(9), MilestoneStatus.RECEIVED), today))
        assertFalse(FreelanceRules.isOverdue(milestone("e", 1, today.minusDays(9), MilestoneStatus.CANCELLED), today))
    }

    @Test
    fun `pending totals leave out received and cancelled milestones and keep currencies apart`() {
        val milestones = listOf(
            milestone("a", 45_000, today),
            milestone("b", 40_000, today.plusDays(10)),
            milestone("c", 20_000, today, MilestoneStatus.RECEIVED),
            milestone("d", 15_000, today, MilestoneStatus.CANCELLED),
            milestone("e", 200, today, currency = Currency.USD),
        )
        val pending = FreelanceRules.pendingByCurrency(milestones)
        assertEquals(listOf(Currency.LKR to lkr(85_000), Currency.USD to lkr(200)), pending)
        assertEquals("LKR 85,000 + USD 200", FreelanceRules.formatPending(pending))
    }

    @Test
    fun `nothing pending gives no text`() {
        assertNull(FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(emptyList())))
    }
}
