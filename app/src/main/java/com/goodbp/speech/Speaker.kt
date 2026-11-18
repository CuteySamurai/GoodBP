package com.goodbp.speech

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.goodbp.data.AppLanguage
import java.util.Locale
import java.util.UUID

/**
 * Speaks prompts. For a fixed phrase it plays a pre-recorded clip
 * (res/raw/<key>_<lang>.ogg, recorded by family in a familiar voice) when one exists,
 * otherwise falls back to TTS. Numbers always go through TTS.
 *
 * Voice quality: phone-maker TTS engines (e.g. on Oppo) often sound robotic or have no
 * proper Mandarin voice. We prefer Google's engine when installed, and pick the
 * best-quality voice for the language.
 */
class Speaker(context: Context, private val language: AppLanguage) {

    private val context = context.applicationContext
    private var ready = false
    private val pending = mutableListOf<() -> Unit>()
    private lateinit var tts: TextToSpeech

    /** Shown in family settings so we can tell why a voice sounds wrong. */
    var status: String = "…"
        private set

    init {
        val onInit = TextToSpeech.OnInitListener { s ->
            ready = s == TextToSpeech.SUCCESS
            if (ready) configure()
            else status = "TTS engine failed to start"
            pending.forEach { it() }; pending.clear()
        }
        tts = if (isInstalled(GOOGLE_TTS)) TextToSpeech(this.context, onInit, GOOGLE_TTS)
        else TextToSpeech(this.context, onInit)
    }

    private fun configure() {
        val locale = Locale.forLanguageTag(language.speechTag)
        val avail = tts.setLanguage(locale)
        val voice = bestVoice(locale)
        if (voice != null) tts.voice = voice
        tts.setSpeechRate(RATE)
        tts.setPitch(1.0f)
        status = buildString {
            append("Engine: ").append(tts.defaultEngine ?: "?")
            append(" | Voice: ").append(voice?.name ?: "default")
            if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
                append(" | ⚠ ").append(language.speechTag).append(" voice missing: tap 'Install voice'")
            }
        }
    }

    /** Highest quality, offline if possible, matching language (and country when it can). */
    private fun bestVoice(locale: Locale): Voice? = runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == locale.language && !it.features.orEmpty().contains("notInstalled") }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country == locale.country }
                    .thenByDescending { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.latency }
            )
            .firstOrNull()
    }.getOrNull()

    private fun isInstalled(pkg: String) =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

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

    fun shutdown() { tts.stop(); tts.shutdown() }

    companion object {
        const val GOOGLE_TTS = "com.google.android.tts"
        const val RATE = 0.9f // a little slower than normal, without sounding dragged out

        /** Opens the engine's "install voice data" screen. */
        fun openInstallVoiceData(context: Context) {
            val i = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(i) }
        }

        /** Play Store page for Google's speech engine, if the phone doesn't have it. */
        fun openGoogleTtsInStore(context: Context) {
            val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$GOOGLE_TTS"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(i) }
        }
    }
}
