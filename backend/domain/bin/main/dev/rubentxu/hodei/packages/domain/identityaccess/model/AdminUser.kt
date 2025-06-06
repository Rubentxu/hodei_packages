package dev.rubentxu.hodei.packages.domain.identityaccess.model

import java.time.Instant
import java.util.UUID

data class AdminUser(
    val id: UUID,
    val username: String,
    val email: String,
    val hashedPassword: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastAccess: Instant?,
)
