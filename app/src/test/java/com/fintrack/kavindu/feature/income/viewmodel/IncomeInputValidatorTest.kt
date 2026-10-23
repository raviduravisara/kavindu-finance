package com.fintrack.kavindu.feature.income.viewmodel

import com.fintrack.kavindu.core.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IncomeInputValidatorTest {

    private val today = LocalDate.of(2026, 10, 25)

    private fun input(
        currency: Currency = Currency.LKR,
        amount: String = "",
        lkrReceived: String = "",
        date: LocalDate = today,
    ) = IncomeFormInput(currency, amount, lkrReceived, date)

    @Test
    fun `salary in LKR is saved as itself at a rate of LKR 1`() {
        val v = IncomeInputValidator.validate(input(amount = "132,500"), today)
        assertTrue(v.isValid)
        assertEquals(13_250_000L, v.amountCents)
        assertEquals(13_250_000L, v.lkrAmountCents)
        assertEquals(100L, v.rateCentsPerUnit)
    }

    @Test
    fun `LKR income ignores the LKR received field`() {
        val v = IncomeInputValidator.validate(input(amount = "1000", lkrReceived = "abc"), today)
        assertTrue(v.isValid)
        assertNull(v.lkrReceivedError)
    }

    @Test
    fun `AdSense USD 50 that became LKR 15,000 stores a rate of LKR 300 per USD`() {
        val v = IncomeInputValidator.validate(input(Currency.USD, "50", "15000"), today)
        assertTrue(v.isValid)
        assertEquals(5_000L, v.amountCents)
        assertEquals(1_500_000L, v.lkrAmountCents)
        assertEquals(30_000L, v.rateCentsPerUnit)
    }

    @Test
    fun `USD without the LKR received cannot be saved`() {
        val v = IncomeInputValidator.validate(input(Currency.USD, "50", ""), today)
        assertFalse(v.isValid)
        assertEquals(IncomeFieldError.LKR_RECEIVED_REQUIRED, v.lkrReceivedError)
    }

    @Test
    fun `USDT with an invalid LKR received shows an invalid error`() {
        val v = IncomeInputValidator.validate(input(Currency.USDT, "120", "12.345"), today)
        assertFalse(v.isValid)
        assertEquals(IncomeFieldError.LKR_RECEIVED_INVALID, v.lkrReceivedError)
    }

    @Test
    fun `empty, zero, negative and text amounts are rejected`() {
        for (text in listOf("", "0", "0.00", "-5", "abc", "12.345")) {
            val v = IncomeInputValidator.validate(input(amount = text), today)
            assertEquals("amount '$text'", IncomeFieldError.AMOUNT_INVALID, v.amountError)
            assertFalse(v.isValid)
        }
    }

    @Test
    fun `a future date is rejected because income is recorded when it arrives`() {
        val v = IncomeInputValidator.validate(input(amount = "1000", date = today.plusDays(1)), today)
        assertEquals(IncomeFieldError.DATE_IN_FUTURE, v.dateError)
        assertFalse(v.isValid)
    }

    @Test
    fun `a past date is fine`() {
        val v = IncomeInputValidator.validate(input(amount = "1000", date = today.minusDays(40)), today)
        assertTrue(v.isValid)
    }

    @Test
    fun `rate rounds to the nearest cent`() {
        // USD 3.00 became LKR 1,000.00 → 333.333... → LKR 333.33 per USD
        assertEquals(33_333L, IncomeInputValidator.rateCentsPerUnit(300, 100_000))
        // USD 3.00 became LKR 1,000.02 → 333.34
        assertEquals(33_334L, IncomeInputValidator.rateCentsPerUnit(300, 100_002))
    }

    @Test
    fun `rate is never zero, so the security rules always accept it`() {
        assertEquals(1L, IncomeInputValidator.rateCentsPerUnit(1_000_000, 1))
    }

    @Test
    fun `amounts are formatted in their own currency`() {
        assertEquals("USD 50", IncomeFormat.amount(5_000, Currency.USD))
        assertEquals("USDT 120.50", IncomeFormat.amount(12_050, Currency.USDT))
        assertEquals("LKR 15,000", IncomeFormat.amount(1_500_000, Currency.LKR))
    }

    @Test
    fun `stored amounts go back into the form without commas`() {
        assertEquals("15000", IncomeFormat.toInputText(1_500_000))
        assertEquals("1250.50", IncomeFormat.toInputText(125_050))
        assertEquals("0.05", IncomeFormat.toInputText(5))
    }
}
