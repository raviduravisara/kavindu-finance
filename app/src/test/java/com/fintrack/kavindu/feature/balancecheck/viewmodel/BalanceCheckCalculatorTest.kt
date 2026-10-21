package com.fintrack.kavindu.feature.balancecheck.viewmodel

import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.CategoryType
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Income
import com.fintrack.kavindu.core.model.IncomeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BalanceCheckCalculatorTest {

    private val checkDate = LocalDate.of(2026, 10, 14)
    private val checkTime = 1_000_000L

    // Last check: LKR 100,000 in the bank + LKR 5,000 cash = LKR 105,000.
    private val lastCheck = BalanceCheck(
        id = "c1",
        date = checkDate,
        bankBalanceCents = 10_000_000,
        walletCashCents = 500_000,
        isStartingPoint = true,
        createdAt = checkTime,
    )

    private fun income(lkrCents: Long, date: LocalDate, createdAt: Long = checkTime + 1) = Income(
        id = "i",
        source = IncomeSource.SALARY,
        amountCents = lkrCents,
        currency = Currency.LKR,
        lkrAmountCents = lkrCents,
        rateCentsPerUnit = 100,
        date = date,
        createdAt = createdAt,
    )

    private fun expense(cents: Long, date: LocalDate, isConfirmed: Boolean = true, createdAt: Long = checkTime + 1) =
        Expense(
            id = "e",
            amountCents = cents,
            date = date,
            categoryId = "food",
            categoryType = CategoryType.DISCRETIONARY,
            channel = "Card",
            account = "Main account",
            isConfirmed = isConfirmed,
            createdAt = createdAt,
        )

    private fun transfer(direction: CryptoDirection, cents: Long, date: LocalDate) = CryptoTransfer(
        id = "t",
        direction = direction,
        amountCents = cents,
        date = date,
        createdAt = checkTime + 1,
    )

    @Test
    fun noNewEntries_expectedIsLastBalance() {
        assertEquals(10_500_000L, BalanceCheckCalculator.expectedBalanceCents(lastCheck, emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun addsIncomeAndSubtractsExpenses() {
        val later = checkDate.plusDays(3)
        val expected = BalanceCheckCalculator.expectedBalanceCents(
            lastCheck,
            incomes = listOf(income(5_000_000, later)), // + LKR 50,000
            expenses = listOf(expense(250_000, later), expense(100_000, later)), // - LKR 3,500
            transfers = emptyList(),
        )
        assertEquals(10_500_000L + 5_000_000 - 350_000, expected)
    }

    @Test
    fun cryptoTransfers_intoSubtracts_outAdds() {
        val later = checkDate.plusDays(1)
        val expected = BalanceCheckCalculator.expectedBalanceCents(
            lastCheck,
            incomes = emptyList(),
            expenses = emptyList(),
            transfers = listOf(
                transfer(CryptoDirection.INTO_BINANCE, 2_000_000, later), // - LKR 20,000
                transfer(CryptoDirection.OUT_OF_BINANCE, 3_000_000, later), // + LKR 30,000
            ),
        )
        assertEquals(10_500_000L - 2_000_000 + 3_000_000, expected)
    }

    @Test
    fun entriesBeforeTheCheck_areNotCounted() {
        val expected = BalanceCheckCalculator.expectedBalanceCents(
            lastCheck,
            incomes = listOf(income(5_000_000, checkDate.minusDays(2))),
            expenses = listOf(expense(250_000, checkDate.minusDays(1))),
            transfers = emptyList(),
        )
        assertEquals(10_500_000L, expected)
    }

    @Test
    fun sameDay_countsOnlyEntriesSavedAfterTheCheck() {
        assertFalse(BalanceCheckCalculator.isAfterCheck(lastCheck, checkDate, createdAt = checkTime))
        assertFalse(BalanceCheckCalculator.isAfterCheck(lastCheck, checkDate, createdAt = checkTime - 1))
        assertTrue(BalanceCheckCalculator.isAfterCheck(lastCheck, checkDate, createdAt = checkTime + 1))
        assertTrue(BalanceCheckCalculator.isAfterCheck(lastCheck, checkDate.plusDays(1), createdAt = 0))
    }

    @Test
    fun unconfirmedEstimate_isNotCounted() {
        val later = checkDate.plusDays(2)
        val expected = BalanceCheckCalculator.expectedBalanceCents(
            lastCheck,
            incomes = emptyList(),
            expenses = listOf(expense(800_000, later, isConfirmed = false)),
            transfers = emptyList(),
        )
        assertEquals(10_500_000L, expected)
    }

    @Test
    fun gap_positiveMeansUnrecordedSpending_negativeMeansExtraMoney() {
        assertEquals(150_000L, BalanceCheckCalculator.gapCents(expectedCents = 1_000_000, actualCents = 850_000))
        assertEquals(-200_000L, BalanceCheckCalculator.gapCents(expectedCents = 1_000_000, actualCents = 1_200_000))
        assertEquals(0L, BalanceCheckCalculator.gapCents(expectedCents = 1_000_000, actualCents = 1_000_000))
    }
}
