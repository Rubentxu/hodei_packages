package dev.rubentxu.hodei.packages.application.security

interface PasswordHasher {
    fun hash(password: String): String

    fun verify(
        password: String,
        hashedPassword: String,
    ): Boolean
}
