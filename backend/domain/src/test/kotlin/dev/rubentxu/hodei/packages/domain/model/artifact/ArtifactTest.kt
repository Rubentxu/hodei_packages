package dev.rubentxu.hodei.packages.domain.model.artifact

import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class ArtifactTest : StringSpec({
    
    "should create a valid Maven artifact" {
        val id = UUID.randomUUID()
        val repositoryId = UUID.randomUUID()
        val now = Instant.now()
        
        val artifact = Artifact(
            id = id,
            repositoryId = repositoryId,
            groupId = "org.example",
            artifactId = "example-lib",
            version = "1.0.0",
            repositoryType = RepositoryType.MAVEN,
            fileSize = 1024L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
            metadata = mapOf("description" to "Sample library")
        )
        
        artifact.id shouldBe id
        artifact.repositoryId shouldBe repositoryId
        artifact.groupId shouldBe "org.example"
        artifact.artifactId shouldBe "example-lib"
        artifact.version shouldBe "1.0.0"
        artifact.repositoryType shouldBe RepositoryType.MAVEN
        artifact.fileSize shouldBe 1024L
        artifact.sha256 shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        artifact.createdAt shouldBe now
        artifact.updatedAt shouldBe now
        artifact.metadata shouldBe mapOf("description" to "Sample library")
    }
    
    "should create a valid NPM artifact" {
        val id = UUID.randomUUID()
        val repositoryId = UUID.randomUUID()
        val now = Instant.now()
        
        val artifact = Artifact(
            id = id,
            repositoryId = repositoryId,
            groupId = "@example",
            artifactId = "ui-lib",
            version = "2.3.1-beta.1",
            repositoryType = RepositoryType.NPM,
            fileSize = 5120L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
            metadata = mapOf("keywords" to "ui, components")
        )
        
        artifact.id shouldBe id
        artifact.repositoryId shouldBe repositoryId
        artifact.groupId shouldBe "@example"
        artifact.artifactId shouldBe "ui-lib"
        artifact.version shouldBe "2.3.1-beta.1"
        artifact.repositoryType shouldBe RepositoryType.NPM
        artifact.fileSize shouldBe 5120L
        artifact.metadata shouldBe mapOf("keywords" to "ui, components")
    }
    
    "should throw exception when group ID is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Artifact(
                id = UUID.randomUUID(),
                repositoryId = UUID.randomUUID(),
                groupId = "",
                artifactId = "example-lib",
                version = "1.0.0",
                repositoryType = RepositoryType.MAVEN,
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        }
        
        exception.message shouldBe "Group ID cannot be blank"
    }
    
    "should throw exception when artifact ID is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Artifact(
                id = UUID.randomUUID(),
                repositoryId = UUID.randomUUID(),
                groupId = "org.example",
                artifactId = "",
                version = "1.0.0",
                repositoryType = RepositoryType.MAVEN,
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        }
        
        exception.message shouldBe "Artifact ID cannot be blank"
    }
    
    "should throw exception when version is invalid" {
        val exception = shouldThrow<IllegalArgumentException> {
            Artifact(
                id = UUID.randomUUID(),
                repositoryId = UUID.randomUUID(),
                groupId = "org.example",
                artifactId = "example-lib",
                version = "invalid-version",
                repositoryType = RepositoryType.MAVEN,
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        }
        
        exception.message shouldBe "Version must follow semantic versioning format"
    }
    
    "should throw exception when file size is negative" {
        val exception = shouldThrow<IllegalArgumentException> {
            Artifact(
                id = UUID.randomUUID(),
                repositoryId = UUID.randomUUID(),
                groupId = "org.example",
                artifactId = "example-lib",
                version = "1.0.0",
                repositoryType = RepositoryType.MAVEN,
                fileSize = -10L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        }
        
        exception.message shouldBe "File size cannot be negative"
    }
    
    "should throw exception when SHA-256 hash is invalid" {
        val exception = shouldThrow<IllegalArgumentException> {
            Artifact(
                id = UUID.randomUUID(),
                repositoryId = UUID.randomUUID(),
                groupId = "org.example",
                artifactId = "example-lib",
                version = "1.0.0",
                repositoryType = RepositoryType.MAVEN,
                fileSize = 1024L,
                sha256 = "invalid-hash",
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        }
        
        exception.message shouldBe "SHA-256 hash must be a valid 64-character hexadecimal string"
    }
})