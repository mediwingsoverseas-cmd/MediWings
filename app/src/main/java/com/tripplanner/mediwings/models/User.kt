package com.tripplanner.mediwings.models

/**
 * User data class representing a user in the MediWings application.
 * Supports both students and workers/admins with role differentiation.
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
