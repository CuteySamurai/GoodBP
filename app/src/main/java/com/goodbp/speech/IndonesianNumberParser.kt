package com.goodbp.speech

/**
 * Turns what the Indonesian speech recogniser heard into numbers.
 * Handles Arabic digits ("120/80 70") and spoken words, including readings said
 * in one breath without labels:
 *   "seratus dua puluh delapan puluh tujuh puluh" -> 120, 80, 70
 *   "seratus tiga puluh lima" -> 135, "seratus sepuluh" -> 110, "sembilan belas" -> 19
 * Labels like atas / bawah / nadi / sistolik are ignored.
 */
object IndonesianNumberParser {

    private sealed interface Tok
    private data class Digit(val v: Int) : Tok          // nol..sembilan
    private data class Literal(val v: Int) : Tok        // Arabic digits
    private data object Puluh : Tok                     // x10
    private data object Belas : Tok                     // +10
    private data object Ratus : Tok                     // x100
    private data object Break : Tok                     // any other word

    private val words = mapOf(
        "nol" to 0, "kosong" to 0, "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4,
        "lima" to 5, "enam" to 6, "tujuh" to 7, "delapan" to 8, "sembilan" to 9,
    )

    private fun tokenize(text: String): List<Tok> {
        val out = mutableListOf<Tok>()
        Regex("[0-9]+|[a-z]+").findAll(text.lowercase()).forEach { m ->
            val w = m.value
            when {
                w[0].isDigit() -> out += Literal(w.toInt())
                w in words -> out += Digit(words.getValue(w))
                w == "puluh" -> out += Puluh
                w == "belas" -> out += Belas
                w == "ratus" -> out += Ratus
                w == "seratus" -> { out += Digit(1); out += Ratus }
                w == "sepuluh" -> { out += Digit(1); out += Puluh }
                w == "sebelas" -> { out += Digit(1); out += Belas }
                // Recognisers sometimes glue "se" on with a space or hyphen: "se ratus"
                w == "se" -> out += Digit(1)
                else -> out += Break
            }
        }
        return out
    }

    fun parseNumbers(text: String): List<Int> {
        val t = tokenize(text)
        val out = mutableListOf<Int>()
        var i = 0
        fun digitAt(k: Int) = (t.getOrNull(k) as? Digit)?.v
        fun isUnit(k: Int) = t.getOrNull(k).let { it == Puluh || it == Belas || it == Ratus }

        while (i < t.size) {
            when (val tok = t[i]) {
                is Literal -> { out += tok.v; i++ }
                is Digit -> {
                    var total = 0
                    // Hundreds: "<d> ratus"
                    if (t.getOrNull(i + 1) == Ratus) {
                        total += tok.v * 100
                        i += 2
                    }
                    // Tens: "<d> puluh" or teens: "<d> belas"
                    var tensDone = false
                    val d = digitAt(i)
                    if (d != null && t.getOrNull(i + 1) == Puluh) {
                        total += d * 10; i += 2; tensDone = true
                        // Ones after tens, unless that digit starts a new "x puluh/belas/ratus"
                        val o = digitAt(i)
                        if (o != null && !isUnit(i + 1)) { total += o; i++ }
                    } else if (d != null && t.getOrNull(i + 1) == Belas) {
                        total += 10 + d; i += 2; tensDone = true
                    }
                    // Bare ones: "seratus lima" = 105, or just "lima"
                    if (!tensDone) {
                        val o = digitAt(i)
                        if (o != null && !isUnit(i + 1)) { total += o; i++ }
                    }
                    out += total
                }
                else -> i++ // Puluh/Belas/Ratus with no digit before it, or a label word
            }
        }
        return out
    }

    /** Systolic, diastolic, pulse, if at least three numbers are found. */
    fun parseReading(text: String): Triple<Int, Int, Int>? {
        val n = parseNumbers(text)
        return if (n.size >= 3) Triple(n[0], n[1], n[2]) else null
    }
}
