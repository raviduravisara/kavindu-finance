package com.fintrack.kavindu.feature.balancecheck.viewmodel

import com.fintrack.kavindu.core.model.BalanceCheck
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import com.fintrack.kavindu.core.model.Expense
import com.fintrack.kavindu.core.model.Income
import java.time.LocalDate

// The weekly balance check maths (S3). Plain Kotlin, so it is unit tested. All amounts are Long cents.
object BalanceCheckCalculator {

    /**
     * An entry counts if it happened after the last check: a later date, or the same day but saved
     * after the check. Entries saved together with the last check (its own "unrecorded spending")
     * share its save time, so they are not counted twice.
     */
    fun isAfterCheck(check: BalanceCheck, date: LocalDate, createdAt: Long): Boolean =
        date.isAfter(check.date) || (date == check.date && createdAt > check.createdAt)

    /**
     * Expected = last check + income since - expenses since - money moved into Binance since
     * + money moved out of Binance since. Income counts the LKR that reached the bank. A utility
     * estimate that is not confirmed yet has not been paid, so it is left out.
     */
    fun expectedBalanceCents(
        lastCheck: BalanceCheck,
        incomes: List<Income>,
        expenses: List<Expense>,
        transfers: List<CryptoTransfer>,
    ): Long {
        val start = lastCheck.bankBalanceCents + lastCheck.walletCashCents
        val incomeIn = incomes
            .filter { isAfterCheck(lastCheck, it.date, it.createdAt) }
            .sumOf { it.lkrAmountCents }
        val spent = expenses
            .filter { it.isConfirmed && isAfterCheck(lastCheck, it.date, it.createdAt) }
            .sumOf { it.amountCents }
        val newTransfers = transfers.filter { isAfterCheck(lastCheck, it.date, it.createdAt) }
        val intoBinance = newTransfers.filter { it.direction == CryptoDirection.INTO_BINANCE }.sumOf { it.amountCents }
        val outOfBinance = newTransfers.filter { it.direction == CryptoDirection.OUT_OF_BINANCE }.sumOf { it.amountCents }
        return start + incomeIn - spent - intoBinance + outOfBinance
    }

    /** Positive: money left that is not in the records (unrecorded spending). Negative: more money than expected. */
    fun gapCents(expectedCents: Long, actualCents: Long): Long = expectedCents - actualCents
}
