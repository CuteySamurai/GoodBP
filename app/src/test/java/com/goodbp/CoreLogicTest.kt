package com.goodbp

import com.goodbp.capture.MonitorTextParser
import com.goodbp.rules.BpCategory
import com.goodbp.rules.BpRange
import com.goodbp.rules.BpResult
import com.goodbp.rules.BpRules
import com.goodbp.speech.MandarinNumberParser
import com.goodbp.ui.flow.FlowState
import com.goodbp.ui.flow.ReadingFlow
import com.goodbp.ui.flow.Via
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    private val range = BpRange(90, 140, 60, 90, BpRange.Source.DOCTOR)

    @Test fun rules() {
        assertEquals(BpResult.NORMAL, BpRules.classify(120, 80, range))
        assertEquals(BpResult.HIGH, BpRules.classify(150, 85, range))
        assertEquals(BpResult.HIGH, BpRules.classify(130, 95, range))
        assertEquals(BpResult.LOW, BpRules.classify(85, 55, range))
        assertEquals(BpResult.LOW, BpRules.classify(150, 55, range)) // low wins
        assertTrue(!BpRules.isPlausible(80, 120, 70))
    }

    @Test fun mandarinNumbers() {
        assertEquals(listOf(120, 80, 70), MandarinNumberParser.parseNumbers("高压一百二十，低压八十，心跳七十"))
        assertEquals(listOf(135, 82, 68), MandarinNumberParser.parseNumbers("135 82 68"))
        assertEquals(listOf(120), MandarinNumberParser.parseNumbers("一二零"))
        assertEquals(listOf(120), MandarinNumberParser.parseNumbers("一百二"))
        assertEquals(listOf(105), MandarinNumberParser.parseNumbers("一百零五"))
        assertEquals(listOf(15), MandarinNumberParser.parseNumbers("十五"))
        assertEquals(listOf(92), MandarinNumberParser.parseNumbers("九十二"))
        assertEquals(listOf(200), MandarinNumberParser.parseNumbers("两百"))
        assertEquals(Triple(98, 58, 64), MandarinNumberParser.parseReading("九十八 五十八 六十四"))
        assertNull(MandarinNumberParser.parseReading("一百二十 八十"))
    }

    @Test fun categories() {
        assertEquals(BpCategory.LOW, BpCategory.of(88, 70))
        assertEquals(BpCategory.LOW, BpCategory.of(110, 58))
        assertEquals(BpCategory.NORMAL, BpCategory.of(118, 76))
        assertEquals(BpCategory.PRE, BpCategory.of(118, 84))
        assertEquals(BpCategory.STAGE1, BpCategory.of(145, 85))
        assertEquals(BpCategory.STAGE2, BpCategory.of(130, 102))
        assertEquals(BpCategory.CRISIS, BpCategory.of(185, 95))
        val f = BpCategory.fraction(130, 70)
        assertTrue(f > 0.4f && f < 0.6f)
    }

    @Test fun ocrConfusions() {
        assertEquals(128, MonitorTextParser.normalise("I28"))
        assertEquals(80, MonitorTextParser.normalise("8O"))
        assertEquals(105, MonitorTextParser.normalise("1O5"))
        assertNull(MonitorTextParser.normalise("SYS"))
        assertNull(MonitorTextParser.normalise("mmHg"))
        val toks = listOf(
            MonitorTextParser.Token("SYS", 10, 10, 20), MonitorTextParser.Token("I32", 40, 100, 120),
            MonitorTextParser.Token("mmHg", 60, 300, 15), MonitorTextParser.Token("8B", 200, 120, 120),
            MonitorTextParser.Token("72", 360, 160, 60), MonitorTextParser.Token("12", 5, 400, 10),
        )
        assertEquals(Triple(132, 88, 72), MonitorTextParser.parseTokens(toks))
    }

    @Test fun monitorOcr() {
        assertEquals(Triple(128, 79, 66), MonitorTextParser.parse(listOf("SYS mmHg", "128", "DIA", "79", "PULSE/min 66")))
        assertNull(MonitorTextParser.parse(listOf("128", "79")))
        assertNull(MonitorTextParser.parse(listOf("79", "128", "66"))) // sys must be > dia
    }

    @Test fun flowSavesOnlyAfterYes() {
        val saved = mutableListOf<BpResult>()
        val f = ReadingFlow(range) { _, _, _, _, r -> saved += r }
        f.photoRead(Triple(85, 55, 70))
        assertEquals(FlowState.ReadBack(85, 55, 70, Via.PHOTO), f.state)
        assertTrue(saved.isEmpty())
        f.confirm(false)
        assertEquals(FlowState.ManualEntry(prefill = Triple(85, 55, 70)), f.state)
        assertTrue(saved.isEmpty())
        f.manualEntered(Triple(86, 56, 71))
        assertEquals(FlowState.ReadBack(86, 56, 71, Via.MANUAL), f.state)
        f.confirm(true)
        assertEquals(listOf(BpResult.LOW), saved)
        assertTrue(f.state is FlowState.Result)
    }

    @Test fun failedPhotoGoesToManualAndBadNumbersStay() {
        val f = ReadingFlow(range) { _, _, _, _, _ -> }
        f.photoRead(null)
        assertEquals(FlowState.ManualEntry(photoFailed = true), f.state)
        f.manualEntered(Triple(80, 120, 70)) // dia > sys: implausible
        assertEquals(FlowState.ManualEntry(prefill = Triple(80, 120, 70), invalid = true), f.state)
        val m = ReadingFlow(range, start = FlowState.ManualEntry()) { _, _, _, _, _ -> }
        assertEquals(FlowState.ManualEntry(), m.state)
    }
}
