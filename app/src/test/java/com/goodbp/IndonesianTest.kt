package com.goodbp

import com.goodbp.data.AppLanguage
import com.goodbp.speech.IndonesianNumberParser
import com.goodbp.speech.NumberParsers
import com.goodbp.speech.YesNo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IndonesianTest {
    private fun n(s: String) = IndonesianNumberParser.parseNumbers(s)

    @Test fun singleNumbers() {
        assertEquals(listOf(120), n("seratus dua puluh"))
        assertEquals(listOf(135), n("seratus tiga puluh lima"))
        assertEquals(listOf(105), n("seratus lima"))
        assertEquals(listOf(110), n("seratus sepuluh"))
        assertEquals(listOf(111), n("seratus sebelas"))
        assertEquals(listOf(115), n("seratus lima belas"))
        assertEquals(listOf(200), n("dua ratus"))
        assertEquals(listOf(212), n("dua ratus dua belas"))
        assertEquals(listOf(80), n("delapan puluh"))
        assertEquals(listOf(72), n("tujuh puluh dua"))
        assertEquals(listOf(19), n("sembilan belas"))
        assertEquals(listOf(10), n("sepuluh"))
        assertEquals(listOf(7), n("tujuh"))
        assertEquals(listOf(100), n("se ratus"))
    }

    @Test fun readingsWithLabels() {
        assertEquals(listOf(120, 80, 70), n("atas seratus dua puluh, bawah delapan puluh, nadi tujuh puluh"))
        assertEquals(listOf(135, 82, 68), n("135 82 68"))
        assertEquals(listOf(140, 90, 75), n("Atas 140 bawah 90 nadi 75"))
        assertEquals(listOf(128, 79, 66), n("128/79 nadi 66"))
        assertEquals(listOf(150, 95, 88), n("sistolik 150 diastolik sembilan puluh lima nadi delapan puluh delapan"))
    }

    @Test fun readingsInOneBreath() {
        // No labels: a digit followed by "puluh" starts the next number
        assertEquals(listOf(120, 80, 70), n("seratus dua puluh delapan puluh tujuh puluh"))
        assertEquals(listOf(128, 85, 72), n("seratus dua puluh delapan delapan puluh lima tujuh puluh dua"))
        assertEquals(listOf(98, 58, 64), n("sembilan puluh delapan lima puluh delapan enam puluh empat"))
        assertEquals(listOf(110, 70, 60), n("seratus sepuluh tujuh puluh enam puluh"))
    }

    @Test fun parseReading() {
        assertEquals(Triple(120, 80, 70), IndonesianNumberParser.parseReading("seratus dua puluh delapan puluh tujuh puluh"))
        assertNull(IndonesianNumberParser.parseReading("seratus dua puluh delapan puluh"))
        assertEquals(Triple(120, 80, 70), NumberParsers.parseReading("seratus dua puluh, delapan puluh, tujuh puluh", AppLanguage.ID))
        assertEquals(Triple(120, 80, 70), NumberParsers.parseReading("一百二十 八十 七十", AppLanguage.ZH))
    }

    @Test fun yesNo() {
        assertEquals(true, YesNo.parse("ya", AppLanguage.ID))
        assertEquals(true, YesNo.parse("iya betul", AppLanguage.ID))
        assertEquals(true, YesNo.parse("Benar", AppLanguage.ID))
        assertEquals(false, YesNo.parse("tidak benar", AppLanguage.ID))
        assertEquals(false, YesNo.parse("bukan", AppLanguage.ID))
        assertEquals(false, YesNo.parse("salah", AppLanguage.ID))
        assertEquals(false, YesNo.parse("nggak", AppLanguage.ID))
        assertNull(YesNo.parse("saya", AppLanguage.ID)) // "ya" inside a word is not yes
        assertNull(YesNo.parse("halo", AppLanguage.ID))
        assertEquals(false, YesNo.parse("不对", AppLanguage.ZH))
        assertEquals(true, YesNo.parse("对", AppLanguage.ZH))
        assertEquals(false, YesNo.parse("no that's wrong", AppLanguage.EN))
        assertEquals(true, YesNo.parse("yes", AppLanguage.EN))
    }

    @Test fun phoneLanguage() {
        assertEquals(AppLanguage.ID, AppLanguage.fromPhone("in"))
        assertEquals(AppLanguage.ID, AppLanguage.fromPhone("id"))
        assertEquals(AppLanguage.ZH, AppLanguage.fromPhone("zh"))
        assertEquals(AppLanguage.EN, AppLanguage.fromPhone("en"))
        assertEquals(AppLanguage.ZH, AppLanguage.fromPhone("fr"))
    }

    private fun keys(dir: String): Map<String, String> {
        val f = listOf(File("src/main/res/$dir/strings.xml"), File("app/src/main/res/$dir/strings.xml")).first { it.exists() }
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(f.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test fun everyStringTranslated() {
        val base = keys("values").keys
        for (dir in listOf("values-zh", "values-in", "values-b+id")) {
            val missing = base - keys(dir).keys
            assertTrue("$dir is missing $missing", missing.isEmpty())
        }
        assertEquals("values-in and values-b+id must match", keys("values-in"), keys("values-b+id"))
    }

    @Test fun indonesianFormatArgsMatch() {
        val en = keys("values"); val id = keys("values-in")
        val arg = Regex("""%\d\$[sd]""")
        for ((k, v) in en) {
            assertEquals("format args differ in $k", arg.findAll(v).map { it.value }.toSet(), arg.findAll(id.getValue(k)).map { it.value }.toSet())
        }
    }
}
