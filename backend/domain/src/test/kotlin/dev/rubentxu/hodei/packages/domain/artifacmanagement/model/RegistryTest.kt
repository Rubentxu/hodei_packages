package dev.rubentxu.hodei.packages.domain.artifacmanagement.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.HostedRegistry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RepositoryType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.StorageConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class RegistryTest : StringSpec({

    fun validStorageConfig() = StorageConfig(
        path = "/tmp/repo",
        blobStoreName = "default",
        strictContentTypeValidation = true,
        storageType = dev.rubentxu.hodei.packages.domain.registrymanagement.model.StorageType.LOCAL
    )

    "should create a valid Maven hosted registry" {
        val id = RegistryId(UUID.randomUUID())
        val registry = HostedRegistry(
            id = id,
            name = "central-maven",
            format = ArtifactType.MAVEN,
            description = "Central Maven repository",
            storageConfig = validStorageConfig()
        )

        registry.id shouldBe id
        registry.name shouldBe "central-maven"
        registry.type shouldBe RepositoryType.HOSTED
        registry.description shouldBe "Central Maven repository"
        registry.format shouldBe ArtifactType.MAVEN
    }

    "should throw exception when repository name is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            HostedRegistry(
                id = RegistryId(UUID.randomUUID()),
                name = "",
                format = ArtifactType.MAVEN,
                description = "Invalid repository",
                storageConfig = validStorageConfig()
            )
        }
        exception.message shouldContain "name"
    }

    "should throw exception when repository name has invalid characters" {
        val exception = shouldThrow<IllegalArgumentException> {
            HostedRegistry(
                id = RegistryId(UUID.randomUUID()),
                name = "invalid/repo",
                format = ArtifactType.MAVEN,
                description = "Invalid repository",
                storageConfig = validStorageConfig()
            )
        }
        exception.message shouldContain "name"
    }

    "should throw exception when repository description exceeds maximum length" {
        val longDescription = "a".repeat(256)
        val exception = shouldThrow<IllegalArgumentException> {
            HostedRegistry(
                id = RegistryId(UUID.randomUUID()),
                name = "maven-repo",
                format = ArtifactType.MAVEN,
                description = longDescription,
                storageConfig = validStorageConfig()
            )
        }
        exception.message shouldContain "description"
    }
})