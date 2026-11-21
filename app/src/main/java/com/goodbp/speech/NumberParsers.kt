package com.goodbp.speech

import com.goodbp.data.AppLanguage

/** Picks the number parser for the app language. Both also read plain digits. */
object NumberParsers {
    fun parseNumbers(text: String, language: AppLanguage): List<Int> = when (language) {
        AppLanguage.ID -> IndonesianNumberParser.parseNumbers(text)
        else -> MandarinNumberParser.parseNumbers(text)
    }

    fun parseReading(text: String, language: AppLanguage): Triple<Int, Int, Int>? {
        val n = parseNumbers(text, language)
        return if (n.size >= 3) Triple(n[0], n[1], n[2]) else null
    }
}

/**
 * Yes / no for the read-back ("Benar?" / "对吗?"). "No" is checked first, because
 * "tidak benar" contains "benar" and 不对 contains 对. Latin words are matched as whole
 * words so "ya" doesn't fire inside "saya".
 */
object YesNo {
    private val zhNo = listOf("不对", "不是", "错")
    private val zhYes = listOf("对", "是", "没错")
    private val idNo = setOf("tidak", "bukan", "salah", "keliru", "nggak", "enggak", "gak", "ga", "ndak", "tak")
    private val idYes = setOf("ya", "iya", "yaa", "betul", "benar", "bener", "ok", "oke", "okay", "sudah", "udah", "yap")
    private val enNo = setOf("no", "nope", "wrong", "incorrect", "not")
    private val enYes = setOf("yes", "yeah", "yep", "correct", "right", "ok", "okay")

    fun parse(text: String, language: AppLanguage): Boolean? {
        val lower = text.lowercase()
        val words = Regex("[a-z]+").findAll(lower).map { it.value }.toSet()
        val no = when (language) {
            AppLanguage.ID -> idNo
            AppLanguage.EN -> enNo
            AppLanguage.ZH -> emptySet()
        }
        val yes = when (language) {
            AppLanguage.ID -> idYes
            AppLanguage.EN -> enYes
            AppLanguage.ZH -> emptySet()
        }
        return when {
            zhNo.any { it in lower } || words.any { it in no } -> false
            zhYes.any { it in lower } || words.any { it in yes } -> true
            else -> null
        }
    }
}
