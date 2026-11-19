package com.goodbp.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Photo logging, all on the phone. First our own seven-segment reader ([SegmentReader]),
 * which reads any monitor that stacks SYS, DIA, PULSE top to bottom. If that fails,
 * ML Kit text recognition, then the optional cloud slot. Null means "type it in".
 * The numbers are always read back for a yes/no before saving.
 */
class PhotoReader(context: Context, private val cloud: CloudReader? = null) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val appContext = context.applicationContext

    /** What OCR saw last time (for the test log / debugging photo problems). */
    var lastRawText: String = ""
        private set

    private val main = Handler(Looper.getMainLooper())

    fun read(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit) {
        Thread {
            val seg = runCatching { loadGray(photo)?.let { SegmentReader.read(it) } }.getOrNull()
            main.post { if (seg != null) { lastRawText = "segments: $seg"; onResult(seg) } else readWithMlKit(photo, onResult) }
        }.start()
    }

    /** Photo as grayscale, upright, about 1000-2000 px on the long side. */
    private fun loadGray(photo: Uri): SegmentReader.Gray? {
        val cr = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(photo)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1000) sample *= 2
        var bmp: Bitmap = cr.openInputStream(photo)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val degrees = runCatching {
            cr.openInputStream(photo)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
        if (degrees != 0) {
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }
        val w = bmp.width; val h = bmp.height
        val argb = IntArray(w * h)
        bmp.getPixels(argb, 0, w, 0, 0, w, h)
        bmp.recycle()
        for (i in argb.indices) {
            val c = argb[i]
            argb[i] = ((c shr 16 and 255) * 299 + (c shr 8 and 255) * 587 + (c and 255) * 114) / 1000
        }
        return SegmentReader.Gray(w, h, argb)
    }

    private fun readWithMlKit(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit) {
        val image = runCatching { InputImage.fromFilePath(appContext, photo) }.getOrNull()
            ?: return onResult(null)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                // Word-level pieces with their boxes, so we can keep the big digits in screen order.
                val tokens = text.textBlocks.flatMap { b -> b.lines.flatMap { l -> l.elements } }.mapNotNull { e ->
                    val box = e.boundingBox ?: return@mapNotNull null
                    MonitorTextParser.Token(e.text, box.top, box.left, box.height())
                }
                val local = MonitorTextParser.parseTokens(tokens)
                    ?: MonitorTextParser.parse(text.textBlocks.sortedBy { it.boundingBox?.top ?: 0 }.flatMap { it.lines.map { l -> l.text } })
                lastRawText = text.text
                if (local != null || cloud == null) onResult(local) else cloud.read(photo, onResult)
            }
            .addOnFailureListener { if (cloud == null) onResult(null) else cloud.read(photo, onResult) }
    }
}

/** Slot for a cloud vision fallback, added only if the on-device test fails. */
fun interface CloudReader {
    fun read(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit)
}
