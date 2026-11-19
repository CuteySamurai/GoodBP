package com.goodbp

import com.goodbp.capture.SegmentReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/** Real monitor photos sent by the family. Expected values are in the file names. */
class SegmentReaderTest {

    private fun load(name: String): SegmentReader.Gray {
        val bi = ImageIO.read(javaClass.getResourceAsStream("/lcd/$name")!!)
        val w = bi.width; val h = bi.height
        val px = IntArray(w * h) {
            val c = bi.getRGB(it % w, it / w)
            ((c shr 16 and 255) * 299 + (c shr 8 and 255) * 587 + (c and 255) * 114) / 1000
        }
        return SegmentReader.Gray(w, h, px)
    }

    @Test fun omronPhotos() {
        assertEquals(Triple(120, 72, 110), SegmentReader.read(load("omron_120_72_110.jpg")))
        assertEquals(Triple(92, 56, 72), SegmentReader.read(load("omron_92_56_72.jpg")))
    }

    @Test fun crookedPhotoStillReads() {
        assertEquals(Triple(92, 56, 72), SegmentReader.read(SegmentReader.rotate(load("omron_92_56_72.jpg"), 5.0)))
    }

    /** Heavy shadow and glare: may not read yet, but must never give wrong numbers. */
    @Test fun hardPhotoNeverWrong() {
        val r = SegmentReader.read(load("guardian_161_110_80.jpg"))
        assertTrue("got $r", r == null || r == Triple(161, 110, 80))
    }
}
