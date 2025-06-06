package dev.rubentxu.hodei.packages.application.auth

/**
 * Command class for registering a new administrator
 */
data class RegisterAdminCommand(
    val username: String,
    val email: String,
    val password: String
)

/**
 * Command class for user login
 */
data class LoginCommand(
    val usernameOrEmail: String,
    val password: String
)

/**
 * Response class containing authentication result data
 */
data class AuthenticationResult(
    val username: String,
    val email: String,
    val token: String,
    val refreshToken: String
)
