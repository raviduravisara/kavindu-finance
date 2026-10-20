package com.fintrack.kavindu.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PayCycleTest {

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun containing_dateBeforeStartDay_belongsToCycleThatStartedLastMonth() {
        val cycle = PayCycle.containing(date(2026, 10, 20), startDay = 25)
        assertEquals(date(2026, 9, 25), cycle.start)
        assertEquals(date(2026, 10, 24), cycle.end)
        assertEquals(30, cycle.lengthInDays)
    }

    @Test
    fun containing_dateOnStartDay_startsNewCycle() {
        val cycle = PayCycle.containing(date(2026, 10, 25), startDay = 25)
        assertEquals(date(2026, 10, 25), cycle.start)
        assertEquals(date(2026, 11, 24), cycle.end)
    }

    @Test
    fun containing_lastDayOfCycle_staysInOldCycle() {
        val cycle = PayCycle.containing(date(2026, 10, 24), startDay = 25)
        assertEquals(date(2026, 9, 25), cycle.start)
    }

    @Test
    fun containing_crossesYearBoundary() {
        val cycle = PayCycle.containing(date(2027, 1, 10), startDay = 25)
        assertEquals(date(2026, 12, 25), cycle.start)
        assertEquals(date(2027, 1, 24), cycle.end)
    }

    @Test
    fun containing_handlesFebruary() {
        val janToFeb = PayCycle.containing(date(2027, 2, 10), startDay = 28)
        assertEquals(date(2027, 1, 28), janToFeb.start)
        assertEquals(date(2027, 2, 27), janToFeb.end)
        assertEquals(31, janToFeb.lengthInDays)

        val febToMar = PayCycle.containing(date(2027, 2, 28), startDay = 28)
        assertEquals(date(2027, 2, 28), febToMar.start)
        assertEquals(date(2027, 3, 27), febToMar.end)
        assertEquals(28, febToMar.lengthInDays)
    }

    @Test
    fun containing_handlesLeapYear() {
        val cycle = PayCycle.containing(date(2028, 2, 29), startDay = 28)
        assertEquals(date(2028, 2, 28), cycle.start)
        assertEquals(date(2028, 3, 27), cycle.end)
        assertEquals(29, cycle.lengthInDays)
    }

    @Test
    fun containing_startDayOne_isCalendarMonth() {
        val cycle = PayCycle.containing(date(2026, 10, 20), startDay = 1)
        assertEquals(date(2026, 10, 1), cycle.start)
        assertEquals(date(2026, 10, 31), cycle.end)
        assertEquals(31, cycle.lengthInDays)
    }

    @Test
    fun containing_rejectsStartDayOutside1To28() {
        assertThrows(IllegalArgumentException::class.java) {
            PayCycle.containing(date(2026, 10, 20), startDay = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PayCycle.containing(date(2026, 10, 20), startDay = 29)
        }
    }

    @Test
    fun isValidStartDay_onlyAllows1To28() {
        assertTrue(PayCycle.isValidStartDay(1))
        assertTrue(PayCycle.isValidStartDay(PayCycle.DEFAULT_START_DAY))
        assertTrue(PayCycle.isValidStartDay(28))
        assertFalse(PayCycle.isValidStartDay(0))
        assertFalse(PayCycle.isValidStartDay(29))
        assertFalse(PayCycle.isValidStartDay(31))
    }

    @Test
    fun contains_includesFirstAndLastDay() {
        val cycle = PayCycle.containing(date(2026, 10, 20), startDay = 25)
        assertTrue(cycle.contains(date(2026, 9, 25)))
        assertTrue(cycle.contains(date(2026, 10, 24)))
        assertFalse(cycle.contains(date(2026, 9, 24)))
        assertFalse(cycle.contains(date(2026, 10, 25)))
    }

    @Test
    fun daysRemaining_countsTodayUntilTheEnd() {
        val cycle = PayCycle.containing(date(2026, 10, 20), startDay = 25)
        assertEquals(5, cycle.daysRemaining(date(2026, 10, 20)))
        assertEquals(1, cycle.daysRemaining(date(2026, 10, 24)))
        assertEquals(0, cycle.daysRemaining(date(2026, 10, 25)))
        assertEquals(30, cycle.daysRemaining(date(2026, 9, 1)))
    }
}
