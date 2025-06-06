package dev.rubentxu.hodei.packages.application.auth

data class LoginCommand(
    val usernameOrEmail: String,
    val password: String,
)
