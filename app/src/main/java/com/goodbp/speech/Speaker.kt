package com.goodbp.speech

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.goodbp.data.AppLanguage
import java.util.Locale
import java.util.UUID

/**
 * Speaks prompts. For a fixed phrase it plays a pre-recorded clip
 * (res/raw/<key>_<lang>.ogg, recorded by family in a familiar voice) when one exists,
 * otherwise falls back to TTS. Numbers always go through TTS.
 */
class Speaker(private val context: Context, private val language: AppLanguage) {

    private var ready = false
    private val pending = mutableListOf<() -> Unit>()
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts.language = Locale.forLanguageTag(language.speechTag)
                tts.setSpeechRate(0.8f) // slower for an elderly listener
                pending.forEach { it() }; pending.clear()
            }
        }
    }

    /** Fixed phrase: clip if recorded, else the localized string via TTS. */
    fun sayPrompt(key: String, fallbackText: String, onDone: () -> Unit = {}) {
        val resId = context.resources.getIdentifier("${key}_${language.tag}", "raw", context.packageName)
        if (resId != 0) {
            MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { it.release(); onDone() }
                start()
            } ?: say(fallbackText, onDone)
        } else {
            say(fallbackText, onDone)
        }
    }

    fun say(text: String, onDone: () -> Unit = {}) {
        if (!ready) { pending += { say(text, onDone) }; return }
        val id = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { if (utteranceId == id) onDone() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { onDone() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun shutdown() { tts.shutdown() }
}
