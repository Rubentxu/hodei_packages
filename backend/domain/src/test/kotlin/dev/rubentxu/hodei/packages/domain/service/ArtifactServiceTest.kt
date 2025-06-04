package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.artifact.ArtifactEvent
import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import dev.rubentxu.hodei.packages.domain.model.registry.Registry
import dev.rubentxu.hodei.packages.domain.model.registry.RegistryType
import dev.rubentxu.hodei.packages.domain.model.registry.StorageType
import dev.rubentxu.hodei.packages.domain.repository.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.repository.RegistryRepository
import io.kotest.assertions.throwables.shouldThrow

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest // Added runTest

import java.time.Instant
import java.util.*

class ArtifactServiceTest : StringSpec({

    // Configuración común
    lateinit var artifactRepository: ArtifactRepository
    lateinit var registryRepository: RegistryRepository
    lateinit var eventPublisher: (ArtifactEvent) -> Unit
    lateinit var artifactService: ArtifactService

    // Datos de prueba comunes
    val registryId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val testRepo = Registry(
        id = registryId,
        name = "test-repo",
        type = RegistryType.MAVEN,
        description = "Test repository",
        isPublic = true,
        createdBy = userId,
        createdAt = Instant.now().minusSeconds(3600),
        updatedAt = Instant.now().minusSeconds(3600),
        storageType = StorageType.LOCAL
    )

    beforeTest {
        artifactRepository = mockk()
        registryRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        artifactService = ArtifactService(artifactRepository, registryRepository, eventPublisher)
    }



    "uploadArtifact should throw exception when repository doesn't exist" { runTest {
        // Arrange

            coEvery { registryRepository.findById(registryId) } returns null


        // Act & Assert
        shouldThrow<IllegalArgumentException> {
            artifactService.uploadArtifact(
                registryId = registryId,
                groupId = "dev.rubentxu",
                artifactId = "test-library",
                version = "1.0.0",
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                metadata = emptyMap(),
                uploadedBy = userId
            )
        }.message shouldBe "ArtifactRegistry with ID '$registryId' not found"

        coVerify { registryRepository.findById(registryId) }
        coVerify(exactly = 0) { artifactRepository.findByCoordinates(any(), any(), any(), any()) }
        coVerify(exactly = 0) { artifactRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    }}

    "uploadArtifact should throw exception for non-existent repository" { runTest {
        // Arrange
        coEvery { registryRepository.findById(registryId) } returns null

        // Act & Assert
        shouldThrow<IllegalArgumentException> {
            artifactService.uploadArtifact(
                registryId = registryId,
                groupId = "dev.rubentxu",
                artifactId = "test-library",
                version = "1.0.0",
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                metadata = emptyMap(),
                uploadedBy = userId
            )
        }.message shouldBe "ArtifactRegistry with ID '$registryId' not found"

        coVerify { registryRepository.findById(registryId) }
        coVerify(exactly = 0) { artifactRepository.findByCoordinates(any(), any(), any(), any()) }
        coVerify(exactly = 0) { artifactRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    }}

    "uploadArtifact should throw exception when artifact already exists" { runTest {
        // Arrange
        val groupId = "dev.rubentxu"
        val artifactId = "test-library"
        val version = "1.0.0"

        val existingArtifact = Artifact(
            id = UUID.randomUUID(),
            registryId = registryId,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            registryType = RegistryType.MAVEN,
            fileSize = 1024L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = userId,
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            metadata = emptyMap()
        )

        coEvery { registryRepository.findById(registryId) } returns testRepo
        coEvery {
            artifactRepository.findByCoordinates(
                registryId,
                groupId,
                artifactId,
                version
            )
        } returns existingArtifact

        // Act & Assert
        shouldThrow<IllegalStateException> {
            artifactService.uploadArtifact(
                registryId = registryId,
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                fileSize = 2048L,
                sha256 = "different-hash",
                metadata = emptyMap(),
                uploadedBy = userId
            )
        }.message shouldBe "Artifact $groupId:$artifactId:$version already exists in artifact registry ${testRepo.name}"

        coVerify { registryRepository.findById(registryId) }
        coVerify { artifactRepository.findByCoordinates(registryId, groupId, artifactId, version) }
        coVerify(exactly = 0) { artifactRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    }}

    "downloadArtifact should emit download event" { runTest {
        // Arrange
        val artifactId = UUID.randomUUID()
        val downloadedBy = UUID.randomUUID()
        val clientIp = "192.168.1.1"
        val userAgent = "Maven/3.8.6"

        val artifact = Artifact(
            id = artifactId,
            registryId = registryId,
            groupId = "dev.rubentxu",
            artifactId = "test-library",
            version = "1.0.0",
            registryType = RegistryType.MAVEN,
            fileSize = 1024L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = userId,
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            metadata = emptyMap()
        )

        coEvery { artifactRepository.findById(artifactId) } returns artifact

        // Act
        val result = artifactService.downloadArtifact(
            artifactId = artifactId,
            downloadedBy = downloadedBy,
            clientIp = clientIp,
            userAgent = userAgent
        )

        // Assert
        result shouldBe artifact

        coVerify { artifactRepository.findById(artifactId) }
        verify {
            eventPublisher(match {
                it is ArtifactEvent.ArtifactDownloaded &&
                        it.artifactId == artifactId &&
                        it.downloadedBy == downloadedBy &&
                        it.clientIp == clientIp &&
                        it.userAgent == userAgent
            })
        }
    }}

    "downloadArtifact should throw exception when artifact doesn't exist" { runTest {
        // Arrange
        val artifactId = UUID.randomUUID()
        coEvery { artifactRepository.findById(artifactId) } returns null

        // Act & Assert
        shouldThrow<IllegalArgumentException> {
            artifactService.downloadArtifact(artifactId)
        }.message shouldBe "Artifact with ID '$artifactId' not found"

        coVerify { artifactRepository.findById(artifactId) }
        verify(exactly = 0) { eventPublisher(any()) }
    }}

    "updateArtifactMetadata should update and persist metadata changes" { runTest {
        // Arrange
        val artifactId = UUID.randomUUID()
        val updatedBy = UUID.randomUUID()
        val existingMetadata = mapOf("key1" to "value1", "key2" to "value2")
        val newMetadata = mapOf("key2" to "new-value2", "key3" to "value3")
        val expectedMetadata = mapOf("key1" to "value1", "key2" to "new-value2", "key3" to "value3")

        val artifact = Artifact(
            id = artifactId,
            registryId = registryId,
            groupId = "dev.rubentxu",
            artifactId = "test-library",
            version = "1.0.0",
            registryType = RegistryType.MAVEN,
            fileSize = 1024L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = userId,
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            metadata = existingMetadata
        )

        coEvery { artifactRepository.findById(artifactId) } returns artifact
        coEvery { artifactRepository.save(any()) } answers {
            (firstArg() as Artifact).copy(updatedAt = Instant.now())
        }

        // Act
        val result = artifactService.updateArtifactMetadata(
            artifactId = artifactId,
            metadata = newMetadata,
            updatedBy = updatedBy
        )

        // Assert
        result.metadata shouldBe expectedMetadata

        coVerify { artifactRepository.findById(artifactId) }
        coVerify { artifactRepository.save(any()) }
        verify {
            eventPublisher(match {
                it is ArtifactEvent.ArtifactMetadataUpdated &&
                        it.artifactId == artifactId &&
                        it.updatedBy == updatedBy &&
                        it.updatedMetadata == newMetadata
            })
        }
    }}

    "deleteArtifact should delete and emit event when artifact exists" { runTest {
        // Arrange
        val artifactId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()

        val artifact = Artifact(
            id = artifactId,
            registryId = registryId,
            groupId = "dev.rubentxu",
            artifactId = "test-library",
            version = "1.0.0",
            registryType = RegistryType.MAVEN,
            fileSize = 1024L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdBy = userId,
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            metadata = emptyMap()
        )

        coEvery { artifactRepository.findById(artifactId) } returns artifact
        coEvery { artifactRepository.deleteById(artifactId) } returns true

        // Act
        val result = artifactService.deleteArtifact(artifactId, deletedBy)

        // Assert
        result shouldBe true

        coVerify { artifactRepository.findById(artifactId) }
        coVerify { artifactRepository.deleteById(artifactId) }
        verify {
            eventPublisher(match {
                it is ArtifactEvent.ArtifactDeleted &&
                        it.artifactId == artifactId &&
                        it.deletedBy == deletedBy
            })
        }
    }}

    "findArtifactsByRepository should return artifacts in the repository" { runTest {
        // Arrange
        val artifacts = listOf(
            Artifact(
                id = UUID.randomUUID(),
                registryId = registryId,
                groupId = "dev.rubentxu",
                artifactId = "library1",
                version = "1.0.0",
                registryType = RegistryType.MAVEN,
                fileSize = 1024L,
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                createdBy = userId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            ),
            Artifact(
                id = UUID.randomUUID(),
                registryId = registryId,
                groupId = "dev.rubentxu",
                artifactId = "library2",
                version = "2.0.0",
                registryType = RegistryType.MAVEN,
                fileSize = 2048L,
                sha256 = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
                createdBy = userId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        )

        coEvery { registryRepository.findById(registryId) } returns testRepo
        coEvery { artifactRepository.findByregistryId(registryId) } returns artifacts


        // Act
        val result = artifactService.findArtifactsByRepository(registryId)


        // Assert
        result shouldBe artifacts

        coVerify { registryRepository.findById(registryId) }
        coVerify { artifactRepository.findByregistryId(registryId) }

    }}

    "findArtifactVersions should return all versions of an artifact" { runTest {
        // Arrange
        val groupId = "dev.rubentxu"
        val artifactId = "test-library"

        val versions = listOf(
            Artifact(
                id = UUID.randomUUID(),
                registryId = registryId,
                groupId = groupId,
                artifactId = artifactId,
                version = "1.0.0",
                registryType = RegistryType.MAVEN,
                fileSize = 1024L,
                sha256 = "0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c4b5a69788796a5b4c3d2e1f0",
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(7200),
                updatedAt = Instant.now().minusSeconds(7200),
                metadata = emptyMap()
            ),
            Artifact(
                id = UUID.randomUUID(),
                registryId = registryId,
                groupId = groupId,
                artifactId = artifactId,
                version = "1.1.0",
                registryType = RegistryType.MAVEN,
                fileSize = 1048L,
                sha256 = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(3600),
                updatedAt = Instant.now().minusSeconds(3600),
                metadata = emptyMap()
            ),
            Artifact(
                id = UUID.randomUUID(),
                registryId = registryId,
                groupId = groupId,
                artifactId = artifactId,
                version = "2.0.0",
                registryType = RegistryType.MAVEN,
                fileSize = 2048L,
                sha256 = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321",
                createdBy = userId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = emptyMap()
            )
        )

        coEvery { registryRepository.findById(registryId) } returns testRepo
        coEvery { artifactRepository.findAllVersions(registryId, groupId, artifactId) } returns versions

        // Act
        val result = artifactService.findArtifactVersions(registryId, groupId, artifactId)

        // Assert
        result shouldBe versions

        coVerify { registryRepository.findById(registryId) }
        coVerify { artifactRepository.findAllVersions(registryId, groupId, artifactId) }
    }}
})