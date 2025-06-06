package dev.rubentxu.hodei.packages.infrastructure.identityaccess

import dev.rubentxu.hodei.packages.domain.identityaccess.ports.PasswordHasher
import org.mindrot.jbcrypt.BCrypt // Import for jBCrypt

class BCryptPasswordHasherAdapter : PasswordHasher {
    override fun hash(password: String): String {
        val salt = BCrypt.gensalt()
        return BCrypt.hashpw(password, salt)
    }

    override fun verify(password: String, hashedPassword: String): Boolean {
        if (hashedPassword.isEmpty()) {
            throw IllegalArgumentException("Invalid salt version")
        }
        try {
            return BCrypt.checkpw(password, hashedPassword)
        } catch (e: IllegalArgumentException) {
            // Catch exceptions from BCrypt.checkpw (e.g., "Invalid salt revision")
            // and re-throw with the message expected by the test.
            throw IllegalArgumentException("Invalid salt version", e)
        }
    }
}
