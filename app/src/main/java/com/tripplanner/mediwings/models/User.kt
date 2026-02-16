package com.tripplanner.mediwings.models

/**
 * User data class for MediWings application.
 * Represents a student/worker user with their profile and documents.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "student",
    val profileImage: String = "",
    val documents: Map<String, String> = emptyMap()
)
