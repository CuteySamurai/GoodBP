package com.goodbp.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.goodbp.data.AppLanguage

/**
 * Wraps Android SpeechRecognizer. Returns all candidate transcripts so callers can
 * pick the first one that parses (accented speech often lands in candidate 2 or 3).
 * Step 0 speech test decides if on-device recognition is good enough for her voice.
 */
class VoiceInput(context: Context, private val language: AppLanguage) {

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

    fun listen(onResult: (List<String>) -> Unit, onError: (Int) -> Unit) {
        val fail = onError
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                onResult(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
            }
            override fun onError(error: Int) = fail(error)
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        })
    }

    fun destroy() = recognizer.destroy()

    companion object {
        /** First candidate that yields three numbers. */
        fun pickReading(candidates: List<String>): Triple<Int, Int, Int>? =
            candidates.firstNotNullOfOrNull { MandarinNumberParser.parseReading(it) }

        /** 对 / 不对 detection. Check "no" first because 不对 contains 对. */
        fun parseYesNo(candidates: List<String>, language: AppLanguage): Boolean? {
            val text = candidates.joinToString(" ").lowercase()
            val no = listOf("不对", "不是", "错", "no", "bukan", "tidak")
            val yes = listOf("对", "是", "没错", "yes", "ya", "betul")
            return when {
                no.any { it in text } -> false
                yes.any { it in text } -> true
                else -> null
            }
        }
    }
}
