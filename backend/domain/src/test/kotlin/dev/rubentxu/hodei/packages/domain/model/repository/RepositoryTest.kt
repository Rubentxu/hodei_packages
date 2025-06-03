package dev.rubentxu.hodei.packages.domain.model.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class RepositoryTest : StringSpec({
    
    "should create a valid Maven repository" {
        val id = UUID.randomUUID()
        val now = Instant.now()
        
        val repository = Repository(
            id = id,
            name = "central-maven",
            type = RepositoryType.MAVEN,
            description = "Central Maven repository",
            createdBy = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
            isPublic = true
        )
        
        repository.id shouldBe id
        repository.name shouldBe "central-maven"
        repository.type shouldBe RepositoryType.MAVEN
        repository.description shouldBe "Central Maven repository"
        repository.createdAt shouldBe now
        repository.updatedAt shouldBe now
        repository.isPublic shouldBe true
    }
    
    "should create a valid NPM repository" {
        val id = UUID.randomUUID()
        val now = Instant.now()
        
        val repository = Repository(
            id = id,
            name = "npm-repo",
            type = RepositoryType.NPM,
            description = "NPM repository",
            createdBy = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
            isPublic = false
        )
        
        repository.id shouldBe id
        repository.name shouldBe "npm-repo"
        repository.type shouldBe RepositoryType.NPM
        repository.description shouldBe "NPM repository"
        repository.createdAt shouldBe now
        repository.updatedAt shouldBe now
        repository.isPublic shouldBe false
    }
    
    "should throw exception when repository name is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Repository(
                id = UUID.randomUUID(),
                name = "",
                type = RepositoryType.MAVEN,
                description = "Invalid repository",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "Repository name cannot be blank"
    }
    
    "should throw exception when repository name has invalid characters" {
        val exception = shouldThrow<IllegalArgumentException> {
            Repository(
                id = UUID.randomUUID(),
                name = "invalid/repo",
                type = RepositoryType.MAVEN,
                description = "Invalid repository",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "Repository name can only contain alphanumeric characters, hyphens, and underscores"
    }
    
    "should throw exception when repository description exceeds maximum length" {
        val longDescription = "a".repeat(256)
        val exception = shouldThrow<IllegalArgumentException> {
            Repository(
                id = UUID.randomUUID(),
                name = "maven-repo",
                type = RepositoryType.MAVEN,
                description = longDescription,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isPublic = true
            )
        }
        
        exception.message shouldBe "Repository description cannot exceed 255 characters"
    }
})