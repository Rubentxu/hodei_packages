package dev.rubentxu.hodei.packages.application.auth

data class AuthenticationResult(
    val message: String,
    // The authentication token
    val token: String? = null,
    val email: String? = null,
    val username: String? = null
)
