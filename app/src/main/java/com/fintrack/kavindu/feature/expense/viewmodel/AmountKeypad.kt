package com.fintrack.kavindu.feature.expense.viewmodel

import com.fintrack.kavindu.core.util.Money

/**
 * The typing rules of the quick-add keypad (FR-18). Plain Kotlin with no Android code, so it is unit tested.
 *
 * The amount is built as text, one key at a time, so the system keyboard never opens and recording an
 * expense stays at amount -> category -> Save (RC2). The text is turned into cents with [Money.parseToCents],
 * never with Double.
 *
 * Rules: no leading zeros ("0" then "5" gives "5"), one decimal point, at most 2 decimals, and at most
 * [MAX_WHOLE_DIGITS] digits before the point (far below the limit the security rules allow).
 */
object AmountKeypad {

    const val MAX_WHOLE_DIGITS = 9
    const val MAX_DECIMALS = 2

    /** [digit] must be '0'..'9'. Returns [current] unchanged when the digit is not allowed. */
    fun appendDigit(current: String, digit: Char): String {
        require(digit in '0'..'9') { "Not a digit: $digit" }
        val dotAt = current.indexOf('.')
        if (dotAt >= 0) {
            val decimals = current.length - dotAt - 1
            return if (decimals >= MAX_DECIMALS) current else current + digit
        }
        return when {
            current == "0" -> digit.toString() // replaces the leading zero, so "0" then "0" stays "0"
            current.length >= MAX_WHOLE_DIGITS -> current
            else -> current + digit
        }
    }

    /** Starts the decimals. A point on an empty amount gives "0." */
    fun appendDot(current: String): String = when {
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }

    /** Removes the last typed key. Backspace on an empty amount does nothing. */
    fun backspace(current: String): String = current.dropLast(1)

    /** The amount in cents, or null while it is empty or zero (Save stays disabled). */
    fun cents(current: String): Long? = Money.parseToCents(current)?.takeIf { it > 0 }

    /**
     * What the big number on the sheet shows: grouped like Money does ("2,500"), "0" while empty, and a
     * typed trailing point or decimals are kept as typed ("2,500." / "2,500.5").
     */
    fun display(current: String): String {
        if (current.isEmpty()) return "0"
        val dotAt = current.indexOf('.')
        val whole = if (dotAt >= 0) current.substring(0, dotAt) else current
        val rest = if (dotAt >= 0) current.substring(dotAt) else ""
        return groupThousands(whole) + rest
    }

    private fun groupThousands(digits: String): String =
        digits.reversed().chunked(3).joinToString(",").reversed()
}
