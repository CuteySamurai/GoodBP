package com.goodbp.rules

/**
 * Reference categories from the family's chart (standard BP table), used only for the
 * colour bar. Fixed rules, no advice. The spoken result still uses her doctor's range.
 *   Low       SYS < 90  or DIA < 60
 *   Normal    SYS < 120 and DIA < 80
 *   Pre-HT    SYS 120-139 or DIA 80-89
 *   Stage 1   SYS 140-159 or DIA 90-99
 *   Stage 2   SYS >= 160 or DIA >= 100
 *   Crisis    SYS > 180 or DIA > 110
 * The higher of the two numbers' categories wins; Low is checked first.
 */
enum class BpCategory { LOW, NORMAL, PRE, STAGE1, STAGE2, CRISIS;

    companion object {
        fun of(sys: Int, dia: Int): BpCategory {
            if (sys < 90 || dia < 60) return LOW
            return maxOf(sysCat(sys), diaCat(dia))
        }
        private fun sysCat(s: Int) = when {
            s > 180 -> CRISIS; s >= 160 -> STAGE2; s >= 140 -> STAGE1; s >= 120 -> PRE; else -> NORMAL
        }
        private fun diaCat(d: Int) = when {
            d > 110 -> CRISIS; d >= 100 -> STAGE2; d >= 90 -> STAGE1; d >= 80 -> PRE; else -> NORMAL
        }

        /** 0..1 position inside the category's segment, from the number that set the category. */
        fun fraction(sys: Int, dia: Int): Float {
            fun f(v: Int, lo: Int, hi: Int) = ((v - lo).toFloat() / (hi - lo)).coerceIn(0.05f, 0.95f)
            return when (of(sys, dia)) {
                LOW -> if (sys < 90) f(sys, 60, 90) else f(dia, 40, 60)
                NORMAL -> maxOf(f(sys, 90, 120), f(dia, 60, 80))
                PRE -> maxOf(if (sys in 120..139) f(sys, 120, 140) else 0f, if (dia in 80..89) f(dia, 80, 90) else 0f)
                STAGE1 -> maxOf(if (sys in 140..159) f(sys, 140, 160) else 0f, if (dia in 90..99) f(dia, 90, 100) else 0f)
                STAGE2 -> maxOf(if (sys in 160..180) f(sys, 160, 181) else 0f, if (dia in 100..110) f(dia, 100, 111) else 0f)
                CRISIS -> maxOf(if (sys > 180) f(sys, 181, 220) else 0f, if (dia > 110) f(dia, 111, 140) else 0f)
            }
        }
    }
}
