package com.goodbp.capture

/**
 * Picks systolic / diastolic / pulse out of OCR text from the monitor screen.
 * The Omron HEM-7120 shows SYS, DIA, PULSE top to bottom, in 7-segment digits that
 * OCR often misreads as letters (0→O/D, 1→I/l, 5→S, 8→B...). We fix those, keep only the
 * big numbers (SYS/DIA are the tallest text; PULSE is smaller but still larger than labels),
 * and take them in top-to-bottom order. Anything doubtful returns null -> manual entry.
 */
object MonitorTextParser {

    /** One piece of OCR text with its on-screen box (pixels). */
    data class Token(val text: String, val top: Int, val left: Int, val height: Int)

    private val confusions = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0', 'U' to '0',
        'I' to '1', 'l' to '1', '|' to '1', 'i' to '1', 'L' to '1', '!' to '1', 'J' to '1',
        'Z' to '2', 'z' to '2', 'E' to '3',
        'A' to '4', 'h' to '4',
        'S' to '5', 's' to '5',
        'G' to '6', 'b' to '6',
        'T' to '7', '/' to '7',
        'B' to '8', '&' to '8',
        'g' to '9', 'q' to '9',
    )

    /** "I2B" -> 128. Only accepts tokens that are mostly digits already. */
    internal fun normalise(raw: String): Int? {
        val t = raw.filterNot { it == ' ' || it == '.' || it == ',' || it == ':' }
        if (t.length !in 2..3) return null
        if (t.count { it.isDigit() } < 1) return null
        val fixed = t.map { if (it.isDigit()) it else confusions[it] ?: return null }.joinToString("")
        return fixed.toIntOrNull()?.takeIf { it in 30..260 }
    }

    /** Main entry: tokens with positions (best). */
    fun parseTokens(tokens: List<Token>): Triple<Int, Int, Int>? {
        val nums = tokens.mapNotNull { t -> normalise(t.text)?.let { it to t } }
        if (nums.size < 3) return null
        val tallest = nums.maxOf { it.second.height }
        val big = nums.filter { it.second.height >= tallest * 0.3 }.sortedWith(compareBy({ it.second.top }, { it.second.left }))
        if (big.size < 3) return null
        val (sys, dia, pulse) = big.take(3).map { it.first }
        return if (sys > dia) Triple(sys, dia, pulse) else null
    }

    /** Fallback: plain lines in reading order. */
    fun parse(lines: List<String>): Triple<Int, Int, Int>? {
        val nums = lines.flatMap { it.split(Regex("""\s+""")) }.mapNotNull { normalise(it) }
        if (nums.size < 3) return null
        val (sys, dia, pulse) = nums
        return if (sys > dia) Triple(sys, dia, pulse) else null
    }
}
