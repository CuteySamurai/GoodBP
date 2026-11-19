package com.goodbp.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Photo logging. On-device ML Kit first. The monitor shows 7-segment digits, which
 * generic OCR can misread; Step 0 photo test decides whether we need the cloud
 * fallback ([CloudReader]). Any failure returns null and the flow switches to voice.
 */
class PhotoReader(context: Context, private val cloud: CloudReader? = null) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val appContext = context.applicationContext

    /** What OCR saw last time (for the test log / debugging photo problems). */
    var lastRawText: String = ""
        private set

    fun read(photo: Uri, onResult: (Triple<Int, Int, Int>?) -> Unit) {
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
