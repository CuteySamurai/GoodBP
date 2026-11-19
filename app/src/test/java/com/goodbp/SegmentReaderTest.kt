package com.goodbp

import com.goodbp.capture.SegmentReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/** Real monitor photos sent by the family. Expected values are in the file names. */
class SegmentReaderTest {

    /** Photos stored as gzip: width, height (big-endian ints), then one grey byte per pixel. */
    private fun load(name: String): SegmentReader.Gray {
        DataInputStream(GZIPInputStream(javaClass.getResourceAsStream("/lcd/$name.gray.gz")!!)).use { s ->
            val w = s.readInt(); val h = s.readInt()
            val bytes = ByteArray(w * h); s.readFully(bytes)
            return SegmentReader.Gray(w, h, IntArray(w * h) { bytes[it].toInt() and 255 })
        }
    }

    @Test fun omronPhotos() {
        assertEquals(Triple(120, 72, 110), SegmentReader.read(load("omron_120_72_110")))
        assertEquals(Triple(92, 56, 72), SegmentReader.read(load("omron_92_56_72")))
    }

    @Test fun guardianPhoto() {
        assertEquals(Triple(160, 103, 84), SegmentReader.read(load("guardian_160_103_84")))
    }

    /** Shadow edge running through the DIA digits. */
    @Test fun shadowedPhoto() {
        assertEquals(Triple(161, 110, 80), SegmentReader.read(load("guardian_161_110_80")))
    }

    @Test fun crookedPhotoStillReads() {
        assertEquals(Triple(92, 56, 72), SegmentReader.read(SegmentReader.rotate(load("omron_92_56_72"), 5.0)))
    }

    /** Heavy shadow or glare: may not read yet, but must never give wrong numbers. */
    @Test fun hardPhotosNeverWrong() {
        val a = SegmentReader.read(load("guardian_161_110_80"))
        assertTrue("got $a", a == null || a == Triple(161, 110, 80))
        // Colour bar gauge on the left once came out as 55/51/55.
        val b = SegmentReader.read(load("guardian_glare_152_107_96"))
        assertTrue("got $b", b == null || b == Triple(152, 107, 96))
        // Diagonal shadow across the DIA row.
        val c = SegmentReader.read(load("shadow_162_102_99"))
        assertTrue("got $c", c == null || c == Triple(162, 102, 99))
    }
}
