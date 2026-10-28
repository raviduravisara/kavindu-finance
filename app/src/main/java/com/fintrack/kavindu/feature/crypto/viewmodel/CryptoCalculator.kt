package com.fintrack.kavindu.feature.crypto.viewmodel

import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.util.Money
import java.time.LocalDate

/** Profit that became real on [date]: the net position rose above its previous highest point. */
data class RealisedProfit(val date: LocalDate, val amountCents: Long)

data class CryptoPosition(
    val totalInCents: Long, // LKR moved into Binance
    val totalOutCents: Long, // LKR moved back out to the bank
    val netCents: Long, // out − in; negative means more is still inside Binance than has come out
    val realisedProfits: List<RealisedProfit>,
) {
    val totalRealisedCents: Long get() = realisedProfits.sumOf { it.amountCents }
}

/**
 * Crypto maths (S2, FR-15 to FR-17). Plain Kotlin, unit tested.
 *
 * Price changes inside Binance are not income: only LKR that actually comes back out counts.
 * Net position = total moved out − total moved in.
 * Realised profit = how far the net position rises above its previous highest value (starting at 0).
 * Example: in 100,000, out 140,000 → net +40,000 → LKR 40,000 realised profit on the day of that cash-out.
 * Profit is calculated from the transfers, never saved as an income entry, so it is never counted twice.
 */
object CryptoCalculator {

    fun position(transfers: List<CryptoTransfer>): CryptoPosition {
        var net = 0L
        var peak = 0L
        var totalIn = 0L
        var totalOut = 0L
        val profits = mutableListOf<RealisedProfit>()
        transfers.sortedWith(compareBy<CryptoTransfer> { it.date }.thenBy { it.createdAt }).forEach { t ->
            when (t.direction) {
                CryptoDirection.INTO_BINANCE -> {
                    totalIn += t.amountCents
                    net -= t.amountCents
                }
                CryptoDirection.OUT_OF_BINANCE -> {
                    totalOut += t.amountCents
                    net += t.amountCents
                }
            }
            if (net > peak) {
                profits += RealisedProfit(t.date, net - peak)
                peak = net
            }
        }
        return CryptoPosition(totalIn, totalOut, net, profits)
    }

    /** Realised profit with a date from [start] to [end], both included (e.g. one pay cycle). */
    fun realisedBetween(position: CryptoPosition, start: LocalDate, end: LocalDate): Long =
        position.realisedProfits.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }.sumOf { it.amountCents }

    /** A transfer amount typed by the user: above 0, at most 2 decimals. Null if invalid. */
    fun parseAmount(text: String): Long? = Money.parseToCents(text)?.takeIf { it > 0 }
}
