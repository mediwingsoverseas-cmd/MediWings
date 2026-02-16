package com.tripplanner.mediwings.models

/**
 * University data class for MediWings application.
 * Represents a Russian medical university.
 */
data class University(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val fees: String = "",
    val imageUrl: String = ""
)
