package dev.rubentxu.hodei.packages.infrastructure.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

class BCryptPasswordHasherAdapterTest : StringSpec({

    val passwordHasher = BCryptPasswordHasherAdapter()

    "hash should return a non-empty string that looks like a BCrypt hash" {
        val password = "mySecurePassword123"
        val hashedPassword = passwordHasher.hash(password)
        
        hashedPassword.shouldNotBeEmpty()
        // BCrypt hashes typically start with $2a$, $2b$, or $2y$ followed by cost factor
        // For simplicity, we check for the common prefix part. A more robust check might involve regex.
        val bcryptPrefix = hashedPassword.substring(0, 4)
        (bcryptPrefix == "\$2a$" || bcryptPrefix == "\$2b$" || bcryptPrefix == "\$2y$") shouldBe true
        hashedPassword.length shouldBe 60 // Standard BCrypt hash length
    }

    "verify should return true for a correct password and its corresponding hash" {
        val knownPassword = "mySecurePassword123"
        // Generate a hash to test against. This ensures the test uses the same hashing logic.
        val knownHash = passwordHasher.hash(knownPassword) 
        passwordHasher.verify(knownPassword, knownHash) shouldBe true
    }

    "verify should return false for an incorrect password and a valid hash" {
        val correctPassword = "mySecurePassword123"
        val incorrectPassword = "wrongPassword"
        val knownHash = passwordHasher.hash(correctPassword)
        passwordHasher.verify(incorrectPassword, knownHash) shouldBe false
    }

    "verify should return false for an empty password and a valid hash" {
        val correctPassword = "mySecurePassword123"
        val emptyPassword = ""
        val knownHash = passwordHasher.hash(correctPassword)
        passwordHasher.verify(emptyPassword, knownHash) shouldBe false
    }

    "verify should throw IllegalArgumentException for malformed hash strings" {
        val exception = shouldThrow<IllegalArgumentException> {
            passwordHasher.verify("anyPassword", "not_a_bcrypt_hash")
        }
        exception.message shouldBe "Invalid salt version"
    }

    "verify should throw IllegalArgumentException for empty hash string" {
        val exception = shouldThrow<IllegalArgumentException> {
            passwordHasher.verify("anyPassword", "")
        }
        exception.message shouldBe "Invalid salt version"
    }
})
