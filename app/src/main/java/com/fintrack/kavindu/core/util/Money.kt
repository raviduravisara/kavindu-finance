package com.fintrack.kavindu.core.util

import kotlin.math.abs

/**
 * Money is always a Long number of cents, never Double or Float.
 * Example: LKR 1,250.50 is stored as 125050.
 */
object Money {

    private const val CENTS_PER_UNIT = 100L

    // 13 digits keeps (whole * 100) far below Long.MAX_VALUE.
    private const val MAX_WHOLE_DIGITS = 13

    /**
     * Formats cents as LKR, as the UI spec shows amounts: "LKR 1,234" for whole amounts and two decimals
     * only when there are cents. For example 49000000 -> "LKR 490,000", 125050 -> "LKR 1,250.50",
     * and -125050 -> "-LKR 1,250.50".
     */
    fun formatLkr(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val absolute = abs(cents)
        val whole = absolute / CENTS_PER_UNIT
        val fraction = absolute % CENTS_PER_UNIT
        val decimals = if (fraction == 0L) "" else ".${fraction.toString().padStart(2, '0')}"
        return "${sign}LKR ${groupThousands(whole)}$decimals"
    }

    /**
     * Turns what the user typed into cents, without using Double.
     * "1250.50", "1,250.5" and "1250" are accepted. Returns null for anything that is not a valid
     * positive amount with at most 2 decimals, for example "", "abc", "-5" or "12.345".
     */
    fun parseToCents(input: String): Long? {
        val text = input.trim().replace(",", "")
        if (text.isEmpty()) return null

        val parts = text.split(".")
        if (parts.size > 2) return null

        val whole = parts[0]
        val fraction = if (parts.size == 2) parts[1] else ""

        if (whole.isEmpty() && fraction.isEmpty()) return null
        if (!whole.all { it in '0'..'9' } || !fraction.all { it in '0'..'9' }) return null
        if (fraction.length > 2 || whole.length > MAX_WHOLE_DIGITS) return null

        val wholeCents = whole.ifEmpty { "0" }.toLong() * CENTS_PER_UNIT
        val fractionCents = fraction.padEnd(2, '0').toLong()
        return wholeCents + fractionCents
    }

    // Adds a comma every three digits, the same on every phone (does not depend on the phone's language).
    private fun groupThousands(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(",").reversed()
}
