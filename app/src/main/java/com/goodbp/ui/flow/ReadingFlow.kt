package com.goodbp.ui.flow

import com.goodbp.rules.BpRange
import com.goodbp.rules.BpResult
import com.goodbp.rules.BpRules

/**
 * The core loop as a plain state machine (no Android), so it can be unit tested:
 *   Photo -> (fail) Voice -> ReadBack -> yes: Result (saved) / no: back to Voice
 * Nothing is saved until she says 对 at read-back. That is a safety rule.
 */
sealed interface FlowState {
    data object TakePhoto : FlowState
    data class SpeakNumbers(val attempt: Int) : FlowState
    data class ReadBack(val sys: Int, val dia: Int, val pulse: Int, val via: Via) : FlowState
    data class Result(val sys: Int, val dia: Int, val pulse: Int, val result: BpResult) : FlowState
    data object HandOffToFamily : FlowState
}

enum class Via { PHOTO, VOICE }

class ReadingFlow(
    private val range: BpRange,
    private val maxVoiceAttempts: Int = 3,
    private val onConfirmedSave: (sys: Int, dia: Int, pulse: Int, via: Via, result: BpResult) -> Unit,
) {
    var state: FlowState = FlowState.TakePhoto
        private set

    fun photoRead(reading: Triple<Int, Int, Int>?) {
        state = accept(reading, Via.PHOTO) ?: FlowState.SpeakNumbers(attempt = 1)
    }

    fun skipPhoto() { state = FlowState.SpeakNumbers(attempt = 1) }

    fun voiceHeard(reading: Triple<Int, Int, Int>?) {
        val s = state as? FlowState.SpeakNumbers ?: return
        state = accept(reading, Via.VOICE) ?: nextVoiceAttempt(s.attempt)
    }

    fun confirm(yes: Boolean) {
        val s = state as? FlowState.ReadBack ?: return
        state = if (yes) {
            val r = BpRules.classify(s.sys, s.dia, range)
            onConfirmedSave(s.sys, s.dia, s.pulse, s.via, r)
            FlowState.Result(s.sys, s.dia, s.pulse, r)
        } else {
            FlowState.SpeakNumbers(attempt = 1)
        }
    }

    private fun accept(r: Triple<Int, Int, Int>?, via: Via): FlowState? =
        r?.takeIf { BpRules.isPlausible(it.first, it.second, it.third) }
            ?.let { FlowState.ReadBack(it.first, it.second, it.third, via) }

    private fun nextVoiceAttempt(attempt: Int): FlowState =
        if (attempt >= maxVoiceAttempts) FlowState.HandOffToFamily else FlowState.SpeakNumbers(attempt + 1)
}
