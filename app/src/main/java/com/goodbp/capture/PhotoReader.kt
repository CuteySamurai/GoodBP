package com.goodbp.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Photo logging, all on the phone: our own seven-segment reader ([SegmentReader]),
 * which reads any monitor that stacks SYS, DIA, PULSE top to bottom. Generic text OCR
 * (ML Kit) was dropped: it misread LCD digits and labels into wrong numbers.
 * Null means "type it in".
 * The numbers are always read back for a yes/no before saving.
 */
class PhotoReader(context: Context, private val cloud: CloudReader? = null) {

    private val appContext = context.applicationContext

    /** What OCR saw last time (for the test log / debugging photo problems). */
    var lastRawText: String = ""
        private set

    private val main = Handler(Looper.getMainLooper())

    fun read(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit) {
        Thread {
            val seg = runCatching { loadGray(photo)?.let { SegmentReader.read(it) } }.getOrNull()
            main.post {
                lastRawText = "segments: $seg"
                if (seg != null || cloud == null) onResult(seg) else cloud.read(photo, onResult)
            }
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
}

/** Slot for a cloud vision fallback, added only if the on-device test fails. */
fun interface CloudReader {
    fun read(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit)
}
