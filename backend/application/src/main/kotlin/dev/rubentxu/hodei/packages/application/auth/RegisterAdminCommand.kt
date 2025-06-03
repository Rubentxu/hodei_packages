package dev.rubentxu.hodei.packages.application.auth

data class RegisterAdminCommand(
    val username: String,
    val email: String,
    val password: String,
)
