package com.tripplanner.mediwings.models

/**
 * TrackingStatus data class for MediWings application.
 * Represents a step in the student's application tracking timeline.
 */
data class TrackingStatus(
    val stepName: String = "",
    val status: String = "pending",
    val completedDate: String = "",
    val remark: String = ""
)
