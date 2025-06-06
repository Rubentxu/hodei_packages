package dev.rubentxu.hodei.packages.domain.model.merkle

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class ContentHashTest : StringSpec({
    "ContentHash should be created with valid algorithm and value" {
        val hash = ContentHash(
            algorithm = "SHA-256",
            value = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
        
        hash.algorithm shouldBe "SHA-256"
        hash.value shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
    
    "ContentHash should validate algorithm is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            ContentHash(
                algorithm = "",
                value = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
        }
        
        exception.message shouldBe "Hash algorithm cannot be blank"
    }
    
    "ContentHash should validate value is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            ContentHash(
                algorithm = "SHA-256",
                value = ""
            )
        }
        
        exception.message shouldBe "Hash value cannot be blank"
    }
    
    "ContentHash.create should generate a SHA-256 hash from bytes" {
        val content = "test content"
        val hash = ContentHash.create(content)
        
        hash.algorithm shouldBe "SHA-256"
        hash.value.shouldNotBeBlank()
        hash.value shouldBe "6ae8a75555209fd6c44157c0aed8016e763ff435a19cf186f76863140143ff72"
    }
    
    "ContentHash.create should generate a SHA-256 hash from string" {
        val content = "test content"
        val hash = ContentHash.create(content)
        
        hash.algorithm shouldBe "SHA-256"
        hash.value shouldBe "6ae8a75555209fd6c44157c0aed8016e763ff435a19cf186f76863140143ff72"
    }
    
    "ContentHash.create should accept a custom algorithm" {
        val content = "test content"
        val hash = ContentHash.create(content, "SHA-512")
        
        hash.algorithm shouldBe "SHA-512"
        hash.value.shouldNotBeBlank()
        // La longitud de un hash SHA-512 en hex es de 128 caracteres
        hash.value.length shouldBe 128
    }
})
