package dev.rubentxu.hodei.packages.domain.identityaccess.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UserId,
    val username: String,
    val email: String,
    val hashedPassword: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastAccess: Instant?,

)
