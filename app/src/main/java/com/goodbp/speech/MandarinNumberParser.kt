package com.goodbp.speech

/**
 * Turns what the speech recogniser heard into numbers.
 * Handles Arabic digits ("120 80 70") and Chinese numerals ("一百二十，八十，七十",
 * "一二零" digit-by-digit, "两"). Labels like 高压/低压/心跳 are ignored.
 */
object MandarinNumberParser {

    private val digits = mapOf(
        '零' to 0, '〇' to 0, '一' to 1, '幺' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )
    private val units = mapOf('十' to 10, '百' to 100)
    private val numeralChars = digits.keys + units.keys

    fun parseNumbers(text: String): List<Int> {
        val out = mutableListOf<Int>()
        val buf = StringBuilder()
        var mode = 0 // 0 none, 1 arabic, 2 chinese
        fun flush() {
            if (buf.isNotEmpty()) {
                val n = if (mode == 1) buf.toString().toIntOrNull() else chineseToInt(buf.toString())
                if (n != null) out += n
                buf.clear()
            }
            mode = 0
        }
        for (c in text) {
            when {
                c.isDigit() -> { if (mode == 2) flush(); mode = 1; buf.append(c) }
                c in numeralChars -> { if (mode == 1) flush(); mode = 2; buf.append(c) }
                else -> flush()
            }
        }
        flush()
        return out
    }

    /** Systolic, diastolic, pulse, if exactly the first three numbers are found. */
    fun parseReading(text: String): Triple<Int, Int, Int>? {
        val n = parseNumbers(text)
        return if (n.size >= 3) Triple(n[0], n[1], n[2]) else null
    }

    internal fun chineseToInt(s: String): Int? {
        if (s.isEmpty()) return null
        // Digit-by-digit style: 一二零 -> 120
        if (s.none { it in units }) {
            return s.map { digits[it] ?: return null }.joinToString("").toIntOrNull()
        }
        var total = 0
        var current = -1
        for (c in s) {
            val d = digits[c]
            val u = units[c]
            when {
                d != null -> current = d
                u != null -> {
                    total += (if (current < 0) 1 else current) * u
                    current = -1
                }
                else -> return null
            }
        }
        if (current > 0) {
            // 一百二 means 120 (trailing digit after 百 is tens)
            total += if (s.length >= 2 && s[s.length - 2] == '百') current * 10 else current
        }
        return total
    }
}
