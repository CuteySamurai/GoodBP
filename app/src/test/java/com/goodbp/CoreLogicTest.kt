package com.goodbp

import com.goodbp.capture.MonitorTextParser
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
        assertEquals(FlowState.SpeakNumbers(1), f.state)
        assertTrue(saved.isEmpty())
        f.voiceHeard(Triple(86, 56, 71))
        f.confirm(true)
        assertEquals(listOf(BpResult.LOW), saved)
        assertTrue(f.state is FlowState.Result)
    }

    @Test fun flowFailedPhotoGoesToVoiceThenFamily() {
        val f = ReadingFlow(range, maxVoiceAttempts = 3) { _, _, _, _, _ -> }
        f.photoRead(null)
        assertEquals(FlowState.SpeakNumbers(1), f.state)
        f.voiceHeard(null); f.voiceHeard(Triple(80, 120, 70)) // implausible counts as a fail
        assertEquals(FlowState.SpeakNumbers(3), f.state)
        f.voiceHeard(null)
        assertEquals(FlowState.HandOffToFamily, f.state)
    }
}
