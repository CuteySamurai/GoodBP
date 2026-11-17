package com.goodbp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Only confirmed readings are stored (she said 对 at read-back). */
@Entity(tableName = "readings")
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val takenAt: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val via: String,      // PHOTO or VOICE
    val result: String,   // NORMAL, HIGH, LOW
    val photoPath: String? = null,
)

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val spokenDescription: String,   // e.g. "白色小药片，降压药"
    val pillPhotoPath: String?,
    val timesOfDay: String,          // "08:00,20:00"
    val requirePhoto: Boolean = false,
)

/**
 * Wording rule from the brief: we record "confirmed by voice" or "photo taken",
 * never "medicine taken".
 */
@Entity(tableName = "med_confirmations")
data class MedConfirmation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val dueAt: Long,
    val confirmedAt: Long?,
    val kind: String,          // VOICE_CONFIRMED, PHOTO_TAKEN, MISSED
    val photoPath: String? = null,
)
