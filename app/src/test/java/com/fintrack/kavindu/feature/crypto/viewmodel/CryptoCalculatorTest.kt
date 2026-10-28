package com.fintrack.kavindu.feature.crypto.viewmodel

import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CryptoCalculatorTest {

    private fun lkr(rupees: Long) = rupees * 100
    private var order = 0L

    private fun into(rupees: Long, date: LocalDate) =
        CryptoTransfer("t${++order}", CryptoDirection.INTO_BINANCE, lkr(rupees), date, null, order)

    private fun out(rupees: Long, date: LocalDate) =
        CryptoTransfer("t${++order}", CryptoDirection.OUT_OF_BINANCE, lkr(rupees), date, null, order)

    private val jan = LocalDate.of(2026, 1, 10)
    private val mar = LocalDate.of(2026, 3, 10)
    private val jul = LocalDate.of(2026, 7, 10)
    private val sep = LocalDate.of(2026, 9, 10)

    @Test
    fun `guide example - in 100,000 and out 140,000 realises 40,000 profit`() {
        val p = CryptoCalculator.position(listOf(into(100_000, jan), out(140_000, mar)))
        assertEquals(lkr(100_000), p.totalInCents)
        assertEquals(lkr(140_000), p.totalOutCents)
        assertEquals(lkr(40_000), p.netCents)
        assertEquals(lkr(40_000), p.totalRealisedCents)
        assertEquals(mar, p.realisedProfits.single().date)
    }

    @Test
    fun `a Q3 loss gives a negative net position and no profit`() {
        val p = CryptoCalculator.position(listOf(into(100_000, jan), out(60_000, jul)))
        assertEquals(lkr(-40_000), p.netCents)
        assertEquals(0L, p.totalRealisedCents)
    }

    @Test
    fun `profit is only counted above the previous high, so it is never counted twice`() {
        val p = CryptoCalculator.position(
            listOf(
                into(100_000, jan), out(140_000, mar), // +40,000 profit
                into(50_000, jul), out(30_000, jul), // net back to +20,000: nothing new
                out(30_000, sep), // net +50,000: 10,000 above the old high of 40,000
            )
        )
        assertEquals(lkr(50_000), p.netCents)
        assertEquals(listOf(lkr(40_000), lkr(10_000)), p.realisedProfits.map { it.amountCents })
        assertEquals(lkr(50_000), p.totalRealisedCents)
    }

    @Test
    fun `transfers are counted in date order, whatever order they arrive in`() {
        val p = CryptoCalculator.position(listOf(out(140_000, mar), into(100_000, jan)))
        assertEquals(lkr(40_000), p.totalRealisedCents)
    }

    @Test
    fun `realised profit can be limited to one pay cycle`() {
        val p = CryptoCalculator.position(listOf(into(100_000, jan), out(140_000, mar), out(10_000, sep)))
        assertEquals(lkr(10_000), CryptoCalculator.realisedBetween(p, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 9, 24)))
        assertEquals(lkr(40_000), CryptoCalculator.realisedBetween(p, mar, mar))
    }

    @Test
    fun `no transfers means everything is zero`() {
        val p = CryptoCalculator.position(emptyList())
        assertEquals(0L, p.netCents)
        assertTrue(p.realisedProfits.isEmpty())
    }

    @Test
    fun `amounts must be above zero`() {
        assertEquals(lkr(25_000), CryptoCalculator.parseAmount("25,000"))
        assertNull(CryptoCalculator.parseAmount("0"))
        assertNull(CryptoCalculator.parseAmount("abc"))
    }
}
