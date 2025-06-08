package dev.rubentxu.hodei.packages.domain.registrymanagement.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.StorageType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class RegistryTest : StringSpec({
    
    "should create a valid Maven registry" {
        val id = UUID.randomUUID()
        val now = Instant.now()
        
        val registry = Registry(
            id = id,
            name = "central-maven",
            type = RegistryType.MAVEN,
            storageType = StorageType.LOCAL,
            description = "Central Maven repository",
            createdBy = UserId(UUID.randomUUID().toString()),
            createdAt = now,
            updatedAt = now,
            isPublic = true
        )
        
        registry.id shouldBe id
        registry.name shouldBe "central-maven"
        registry.type shouldBe RegistryType.MAVEN
        registry.storageType shouldBe StorageType.LOCAL
        registry.description shouldBe "Central Maven repository"
        registry.createdAt shouldBe now
        registry.updatedAt shouldBe now
        registry.isPublic shouldBe true
    }
    
    "should create a valid NPM registry" {
        val id = UUID.randomUUID()
        val now = Instant.now()
        
        val registry = Registry(
            id = id,
            name = "npm-repo",
            type = RegistryType.NPM,
            storageType = StorageType.LOCAL,
            description = "NPM repository",
            createdBy = UserId(UUID.randomUUID().toString()),
            createdAt = now,
            updatedAt = now,
            isPublic = false
        )
        
        registry.id shouldBe id
        registry.name shouldBe "npm-repo"
        registry.type shouldBe RegistryType.NPM
        registry.storageType shouldBe StorageType.LOCAL
        registry.description shouldBe "NPM repository"
        registry.createdAt shouldBe now
        registry.updatedAt shouldBe now
        registry.isPublic shouldBe false
    }
    
    "should throw exception when registry name is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Registry(
                id = UUID.randomUUID(),
                name = "",
                type = RegistryType.MAVEN,
                storageType = StorageType.LOCAL,
                description = "Invalid registry",
                createdBy = UserId(UUID.randomUUID().toString()),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "ArtifactRegistry name cannot be blank"
    }
    
    "should throw exception when registry name has invalid characters" {
        val exception = shouldThrow<IllegalArgumentException> {
            Registry(
                id = UUID.randomUUID(),
                name = "invalid/repo",
                type = RegistryType.MAVEN,
                storageType = StorageType.LOCAL,
                description = "Invalid registry",
                createdBy = UserId(UUID.randomUUID().toString()),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "ArtifactRegistry name can only contain alphanumeric characters, hyphens, and underscores"
    }
    
    "should throw exception when registry description exceeds maximum length" {
        val longDescription = "a".repeat(256)
        val exception = shouldThrow<IllegalArgumentException> {
            Registry(
                id = UUID.randomUUID(),
                name = "maven-repo",
                type = RegistryType.MAVEN,
                storageType = StorageType.LOCAL,
                description = longDescription,
                createdBy = UserId(UUID.randomUUID().toString()),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "ArtifactRegistry description cannot exceed 255 characters"
    }
})