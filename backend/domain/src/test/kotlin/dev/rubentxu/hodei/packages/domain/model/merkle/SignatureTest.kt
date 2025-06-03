package dev.rubentxu.hodei.packages.domain.model.merkle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.Instant

class SignatureTest : StringSpec({
    val contentHash = ContentHash("SHA-256", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    val creationTime = Instant.parse("2023-01-01T12:00:00Z")
    
    "Signature should be created with required fields" {
        val signature = Signature(
            value = "abc123signature456xyz",
            algorithm = "Ed25519",
            contentHash = contentHash,
            keyId = "user1@example.com",
            creationTime = creationTime
        )
        
        signature.value shouldBe "abc123signature456xyz"
        signature.algorithm shouldBe "Ed25519"
        signature.contentHash shouldBe contentHash
        signature.keyId shouldBe "user1@example.com"
        signature.creationTime shouldBe creationTime
        signature.id.shouldNotBeBlank()
    }
    
    "Signature should validate value is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Signature(
                value = "",
                algorithm = "Ed25519",
                contentHash = contentHash,
                keyId = "user1@example.com"
            )
        }
        
        exception.message shouldBe "Signature value cannot be blank"
    }
    
    "Signature should validate algorithm is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Signature(
                value = "abc123signature456xyz",
                algorithm = "",
                contentHash = contentHash,
                keyId = "user1@example.com"
            )
        }
        
        exception.message shouldBe "Signature algorithm cannot be blank"
    }
    
    "Signature should validate keyId is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Signature(
                value = "abc123signature456xyz",
                algorithm = "Ed25519",
                contentHash = contentHash,
                keyId = ""
            )
        }
        
        exception.message shouldBe "Key ID cannot be blank"
    }
    
    "Signature should generate consistent id based on value and creationTime" {
        val sig1 = Signature(
            value = "abc123signature456xyz",
            algorithm = "Ed25519",
            contentHash = contentHash,
            keyId = "user1@example.com",
            creationTime = creationTime
        )
        
        val sig2 = Signature(
            value = "abc123signature456xyz",
            algorithm = "Ed25519",
            contentHash = contentHash,
            keyId = "user1@example.com",
            creationTime = creationTime
        )
        
        sig1.id shouldBe sig2.id
    }
})
