package com.tripplanner.mediwings.models

/**
 * TrackingStatus data class representing a step in the student's application timeline.
 * Used for tracking progress from Application to Flight booking.
 */
data class TrackingStatus(
    val stepName: String = "",
    val status: String = "pending",
    val completedDate: String = "",
    val remark: String = ""
)
