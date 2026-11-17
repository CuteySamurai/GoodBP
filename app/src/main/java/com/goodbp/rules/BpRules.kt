package com.goodbp.rules

/**
 * Fixed rules only. This classifies a reading against a range the family enters
 * (from her doctor). It never diagnoses and never gives medicine advice.
 */
enum class BpResult { NORMAL, HIGH, LOW }

data class BpRange(
    val sysLow: Int, val sysHigh: Int,
    val diaLow: Int, val diaHigh: Int,
    val source: Source,
) {
    enum class Source { AGE_DEFAULT, DOCTOR }
}

object BpRules {

    /**
     * Placeholder defaults pre-filled from age, so the app works before the doctor's
     * numbers are entered. The family MUST replace these with the doctor's range.
     * TODO: confirm defaults with her doctor; values here are generic placeholders.
     */
    fun defaultRangeFor(age: Int): BpRange = when {
        age >= 65 -> BpRange(sysLow = 100, sysHigh = 150, diaLow = 60, diaHigh = 90, source = BpRange.Source.AGE_DEFAULT)
        else -> BpRange(sysLow = 90, sysHigh = 140, diaLow = 60, diaHigh = 90, source = BpRange.Source.AGE_DEFAULT)
    }

    /** LOW wins over HIGH: low blood pressure is what made her faint. */
    fun classify(sys: Int, dia: Int, range: BpRange): BpResult = when {
        sys < range.sysLow || dia < range.diaLow -> BpResult.LOW
        sys > range.sysHigh || dia > range.diaHigh -> BpResult.HIGH
        else -> BpResult.NORMAL
    }

    /** Sanity limits for what a home monitor can show; outside these we ask again. */
    fun isPlausible(sys: Int, dia: Int, pulse: Int): Boolean =
        sys in 50..260 && dia in 30..180 && pulse in 30..220 && sys > dia
}
