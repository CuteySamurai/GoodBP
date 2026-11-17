package com.goodbp.capture

/**
 * Picks systolic / diastolic / pulse out of OCR text from the monitor screen.
 * Omron HEM-7120 shows SYS, DIA, PULSE top to bottom, so the order of large
 * numbers on screen is the order we want. OCR blocks are passed in reading order.
 * Anything doubtful returns null so the app falls back to voice.
 */
object MonitorTextParser {

    private val number = Regex("""(?<!\d)\d{2,3}(?!\d)""")

    fun parse(lines: List<String>): Triple<Int, Int, Int>? {
        val nums = lines.flatMap { line -> number.findAll(line).map { it.value.toInt() }.toList() }
            .filter { it in 30..260 }
        if (nums.size < 3) return null
        val (sys, dia, pulse) = nums
        return if (sys > dia) Triple(sys, dia, pulse) else null
    }
}
