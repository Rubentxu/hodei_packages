package dev.rubentxu.hodei.packages.application.auth

data class AuthenticationResult(
    val username: String,
    val email: String,
    val token: String,
    val message: String
)
