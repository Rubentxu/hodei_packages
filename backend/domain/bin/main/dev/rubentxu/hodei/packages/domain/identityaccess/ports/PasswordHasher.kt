package dev.rubentxu.hodei.packages.domain.identityaccess.ports

interface PasswordHasher {
    fun hash(password: String): String

    fun verify(
        password: String,
        hashedPassword: String,
    ): Boolean
}
