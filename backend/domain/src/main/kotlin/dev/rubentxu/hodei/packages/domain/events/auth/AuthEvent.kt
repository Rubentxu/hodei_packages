package dev.rubentxu.hodei.packages.domain.events.auth

import java.time.Instant
import java.util.UUID

sealed class AuthEvent {
    data class AdminRegistered(
        val adminId: UUID,
        val username: String,
        val email: String,
        val timestamp: Instant,
    ) : AuthEvent()

    data class LoginAttempt(
        val identifier: String,
        val success: Boolean,
        val timestamp: Instant,
    ) : AuthEvent()

    data class PasswordChanged(
        val adminId: UUID,
        val timestamp: Instant,
    ) : AuthEvent()
}
