package dev.rubentxu.hodei.packages.application.security

interface TokenService {
    /**
     * Generates a new authentication token.
     *
     * @param userId The unique identifier of the user.
     * @param username The username of the user.
     * @param email The email of the user.
     * @return A string representation of the generated token.
     */
    fun generateToken(
        userId: String,
        username: String,
        email: String,
    ): String
}
