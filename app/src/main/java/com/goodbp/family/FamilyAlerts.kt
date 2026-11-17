package com.goodbp.family

import com.goodbp.rules.BpResult

/**
 * Family alerts (version 1 = caretaker mode push). Stub interface for now; the
 * Firebase step implements it with Firestore + FCM. Local no-op keeps the app usable offline.
 */
interface FamilyAlerts {
    fun readingOutOfRange(sys: Int, dia: Int, pulse: Int, result: BpResult)
    fun readingMissed(dueAt: Long)
    fun medicineNotConfirmed(medicineName: String, dueAt: Long)
    fun needsHelp(reason: String) // e.g. voice failed 3 times
}

object NoOpFamilyAlerts : FamilyAlerts {
    override fun readingOutOfRange(sys: Int, dia: Int, pulse: Int, result: BpResult) {}
    override fun readingMissed(dueAt: Long) {}
    override fun medicineNotConfirmed(medicineName: String, dueAt: Long) {}
    override fun needsHelp(reason: String) {}
}
