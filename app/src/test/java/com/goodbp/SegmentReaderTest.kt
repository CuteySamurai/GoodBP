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

    @Test fun crookedPhotoStillReads() {
        assertEquals(Triple(92, 56, 72), SegmentReader.read(SegmentReader.rotate(load("omron_92_56_72"), 5.0)))
    }

    /** Heavy shadow and glare: may not read yet, but must never give wrong numbers. */
    @Test fun hardPhotoNeverWrong() {
        val r = SegmentReader.read(load("guardian_161_110_80"))
        assertTrue("got $r", r == null || r == Triple(161, 110, 80))
    }
}
