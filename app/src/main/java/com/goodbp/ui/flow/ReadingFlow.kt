package com.goodbp.ui.flow

import com.goodbp.rules.BpRange
import com.goodbp.rules.BpResult
import com.goodbp.rules.BpRules

/**
 * The core loop as a plain state machine (no Android), so it can be unit tested:
 *   Photo --(fail)--> Manual entry --> ReadBack --yes--> Result (saved)
 *                                         \--no--> Manual entry (numbers pre-filled)
 * Nothing is saved until she confirms at read-back. That is a safety rule.
 */
sealed interface FlowState {
    data object TakePhoto : FlowState
    data class ManualEntry(val prefill: Triple<Int, Int, Int>? = null, val invalid: Boolean = false) : FlowState
    data class ReadBack(val sys: Int, val dia: Int, val pulse: Int, val via: Via) : FlowState
    data class Result(val sys: Int, val dia: Int, val pulse: Int, val result: BpResult) : FlowState
}

enum class Via { PHOTO, MANUAL }

class ReadingFlow(
    private val range: BpRange,
    start: FlowState = FlowState.TakePhoto,
    private val onConfirmedSave: (sys: Int, dia: Int, pulse: Int, via: Via, result: BpResult) -> Unit,
) {
    var state: FlowState = start
        private set

    fun photoRead(reading: Triple<Int, Int, Int>?) {
        state = accept(reading, Via.PHOTO) ?: FlowState.ManualEntry()
    }

    fun skipPhoto() { state = FlowState.ManualEntry() }

    fun manualEntered(reading: Triple<Int, Int, Int>) {
        state = accept(reading, Via.MANUAL) ?: FlowState.ManualEntry(prefill = reading, invalid = true)
    }

    fun confirm(yes: Boolean) {
        val s = state as? FlowState.ReadBack ?: return
        state = if (yes) {
            val r = BpRules.classify(s.sys, s.dia, range)
            onConfirmedSave(s.sys, s.dia, s.pulse, s.via, r)
            FlowState.Result(s.sys, s.dia, s.pulse, r)
        } else {
            FlowState.ManualEntry(prefill = Triple(s.sys, s.dia, s.pulse))
        }
    }

    private fun accept(r: Triple<Int, Int, Int>?, via: Via): FlowState? =
        r?.takeIf { BpRules.isPlausible(it.first, it.second, it.third) }
            ?.let { FlowState.ReadBack(it.first, it.second, it.third, via) }
}
