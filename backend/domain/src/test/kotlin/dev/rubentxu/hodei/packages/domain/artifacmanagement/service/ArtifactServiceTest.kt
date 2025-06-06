package dev.rubentxu.hodei.packages.domain.artifacmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.MerkleRootHash
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.SignatureId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.service.ArtifactService
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.Registry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.StorageType
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.UUID

class ArtifactServiceTest : StringSpec({

    // Configuración común
    lateinit var artifactRepository: ArtifactRepository
    lateinit var registryRepository: RegistryRepository
    lateinit var publishArtifactEvent: (ArtifactPublishedEvent) -> Unit
    lateinit var publishDownloadEvent: (ArtifactDownloadedEvent) -> Unit
    lateinit var publishMetadataUpdateEvent: (ArtifactMetadataUpdatedEvent) -> Unit
    lateinit var publishDeleteEvent: (ArtifactDeletedEvent) -> Unit
    lateinit var artifactService: ArtifactService

    // Datos de prueba comunes
    val registryId = UUID.randomUUID()
    val userId = UserId("user-123")
    val testRepo = Registry(
        id = registryId,
        name = "test-repo",
        description = "Test repository",
        storageType = StorageType.LOCAL,
        isPublic = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        type = RegistryType.HELM,
        createdBy = userId
    )

    beforeTest {
        artifactRepository = mockk()
        registryRepository = mockk()
        publishArtifactEvent = mockk(relaxed = true)
        publishDownloadEvent = mockk(relaxed = true)
        publishMetadataUpdateEvent = mockk(relaxed = true)
        publishDeleteEvent = mockk(relaxed = true)

        artifactService = ArtifactService(
            artifactRepository = artifactRepository,
            registryRepository = registryRepository,
            publishArtifactEvent = publishArtifactEvent,
            publishDownloadEvent = publishDownloadEvent,
            publishMetadataUpdateEvent = publishMetadataUpdateEvent,
            publishDeleteEvent = publishDeleteEvent
        )
    }

    "publishArtifact should return failure when repository doesn't exist" {
        runTest {
            // Arrange
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = "abcdef1234567890abcdef1234567890"

            coEvery { registryRepository.findById(registryId) } returns Result.success(null)

            // Act
            val result = artifactService.publishArtifact(
                registryId = registryId,
                group = coordinates.group,
                name = coordinates.name,
                version = coordinates.version,
                createdBy = userId,
                merkleRoot = merkleRootHash
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Registry with ID '$registryId' not found"

            coVerify { registryRepository.findById(registryId) }
            coVerify(exactly = 0) { artifactRepository.save(any()) }
            verify(exactly = 0) { publishArtifactEvent(any()) }
        }
    }

    "publishArtifact should return failure when repository is not active" {
        runTest {
            // Arrange
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = "abcdef1234567890abcdef1234567890"

            coEvery { registryRepository.findById(registryId) } returns Result.success(testRepo)
            coEvery { registryRepository.isRepositoryActive(registryId) } returns false

            // Act
            val result = artifactService.publishArtifact(
                registryId = registryId,
                group = coordinates.group,
                name = coordinates.name,
                version = coordinates.version,
                createdBy = userId,
                merkleRoot = merkleRootHash
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Registry 'test-repo' (ID: $registryId) is not active."

            coVerify { registryRepository.findById(registryId) }
            coVerify { registryRepository.isRepositoryActive(registryId) }
            coVerify(exactly = 0) { artifactRepository.save(any()) }
            verify(exactly = 0) { publishArtifactEvent(any()) }
        }
    }

    "publishArtifact should return failure when artifact already exists" {
        runTest {
            // Arrange
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = "abcdef1234567890abcdef1234567890"
            val existingArtifact = Artifact(
                id = ArtifactId("existing-id"),
                coordinates = coordinates,
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(3600),
                merkleRoot = MerkleRootHash(merkleRootHash)
            )

            coEvery { registryRepository.findById(registryId) } returns Result.success(testRepo)
            coEvery { registryRepository.isRepositoryActive(registryId) } returns true
            coEvery { artifactRepository.findByCoordinates(coordinates) } returns Result.success(existingArtifact)

            // Act
            val result = artifactService.publishArtifact(
                registryId = registryId,
                group = coordinates.group,
                name = coordinates.name,
                version = coordinates.version,
                createdBy = userId,
                merkleRoot = merkleRootHash
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()!!.message shouldBe "Artifact org.example:test-artifact:1.0.0 already exists"

            coVerify { registryRepository.findById(registryId) }
            coVerify { registryRepository.isRepositoryActive(registryId) }
            coVerify { artifactRepository.findByCoordinates(coordinates) }
            coVerify(exactly = 0) { artifactRepository.save(any()) }
            verify(exactly = 0) { publishArtifactEvent(any()) }
        }
    }

    "publishArtifact should successfully create and save a new artifact" {
        runTest {
            // Arrange
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = "abcdef1234567890abcdef1234567890"

            coEvery { registryRepository.findById(registryId) } returns Result.success(testRepo)
            coEvery { registryRepository.isRepositoryActive(registryId) } returns true
            coEvery { artifactRepository.findByCoordinates(coordinates) } returns Result.success(null)
            coEvery { artifactRepository.save(any()) } answers {
                val artifact = firstArg<Artifact>()
                Result.success(artifact) // Devolver el mismo artifact que recibimos
            }

            // Act
            val result = artifactService.publishArtifact(
                registryId = registryId,
                group = coordinates.group,
                name = coordinates.name,
                version = coordinates.version,
                createdBy = userId,
                merkleRoot = merkleRootHash
            )

            // Assert
            result.isSuccess shouldBe true
            val artifact = result.getOrNull()!!

            with(artifact) {
                this.coordinates shouldBe coordinates
                this.merkleRoot?.value shouldBe merkleRootHash
                this.createdBy shouldBe userId
            }

            coVerify { artifactRepository.findByCoordinates(coordinates) }

            val artifactSlot = slot<Artifact>()
            coVerify { artifactRepository.save(capture(artifactSlot)) }

            // Verificar que se publicó el evento correcto
            val eventSlot = slot<ArtifactPublishedEvent>()
            verify { publishArtifactEvent(capture(eventSlot)) }

            with(eventSlot.captured) {
                publishedBy shouldBe userId
            }
        }
    }

    "downloadArtifact should return failure when artifact doesn't exist" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("non-existent-id")
            val clientIp = "192.168.1.1"
            val userAgent = "Test-Agent/1.0"

            coEvery { artifactRepository.findById(artifactId) } returns Result.success(null)

            // Act
            val result = artifactService.downloadArtifact(
                artifactId = artifactId,
                downloadedBy = userId,
                clientIp = clientIp,
                userAgent = userAgent
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "Artifact with ID '${artifactId.value}' not found"

            coVerify { artifactRepository.findById(artifactId) }
            verify(exactly = 0) { publishDownloadEvent(any()) }
        }
    }

    "downloadArtifact should record download and publish event" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("art-123")
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = MerkleRootHash("abcdef1234567890abcdef1234567890")
            val clientIp = "192.168.1.1"
            val userAgent = "Test-Agent/1.0"

            val artifact = Artifact(
                id = artifactId,
                coordinates = coordinates,
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(3600),
                merkleRoot = merkleRootHash,
                policies = listOf(PolicyId("policy-123")),
                sbomIds = listOf(SbomId("sbom-123")),
                signatureIds = listOf(SignatureId("sig-123"))
            )

            coEvery { artifactRepository.findById(artifactId) } returns Result.success(artifact)

            // Act
            val result = artifactService.downloadArtifact(
                artifactId = artifactId,
                downloadedBy = userId,
                clientIp = clientIp,
                userAgent = userAgent
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe artifact

            // Verificar que se publicó el evento correcto
            val eventSlot = slot<ArtifactDownloadedEvent>()
            verify { publishDownloadEvent(capture(eventSlot)) }

            with(eventSlot.captured) {
                this.artifactId shouldBe artifactId
                this.downloadedBy shouldBe userId
                this.clientIp shouldBe clientIp
                this.userAgent shouldBe userAgent
            }
        }
    }

    "deleteArtifact should return success with false when artifact doesn't exist" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("non-existent-id")

            coEvery { artifactRepository.findById(artifactId) } returns Result.success(null)

            // Act
            val result = artifactService.deleteArtifact(
                artifactId = artifactId,
                deletedBy = userId
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe false

            coVerify { artifactRepository.findById(artifactId) }
            coVerify(exactly = 0) { artifactRepository.deleteById(any()) }
            verify(exactly = 0) { publishDeleteEvent(any()) }
        }
    }

    "deleteArtifact should delete artifact and publish event" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("art-123")
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = MerkleRootHash("abcdef1234567890abcdef1234567890")

            val artifact = Artifact(
                id = artifactId,
                coordinates = coordinates,
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(3600),
                merkleRoot = merkleRootHash
            )

            coEvery { artifactRepository.findById(artifactId) } returns Result.success(artifact)
            coEvery { artifactRepository.deleteById(artifactId) } returns true

            // Act
            val result = artifactService.deleteArtifact(
                artifactId = artifactId,
                deletedBy = userId
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe true

            coVerify { artifactRepository.findById(artifactId) }
            coVerify { artifactRepository.deleteById(artifactId) }

            // Verificar que se publicó el evento correcto
            val eventSlot = slot<ArtifactDeletedEvent>()
            verify { publishDeleteEvent(capture(eventSlot)) }

            with(eventSlot.captured) {
                this.artifactId shouldBe artifactId
                this.deletedBy shouldBe userId
            }
        }
    }

    "getAllVersions should return list of artifacts with matching group and name" {
        runTest {
            // Arrange
            val group = "org.example"
            val name = "test-artifact"

            val artifacts = listOf(
                Artifact(
                    id = ArtifactId("art-123"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "1.0.0"),
                    createdBy = userId,
                    createdAt = Instant.now().minusSeconds(7200),
                    merkleRoot = MerkleRootHash("hash1")
                ),
                Artifact(
                    id = ArtifactId("art-456"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "1.1.0"),
                    createdBy = userId,
                    createdAt = Instant.now().minusSeconds(3600),
                    merkleRoot = MerkleRootHash("hash2")
                ),
                Artifact(
                    id = ArtifactId("art-789"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "2.0.0"),
                    createdBy = userId,
                    createdAt = Instant.now(),
                    merkleRoot = MerkleRootHash("hash3")
                )
            )

            coEvery { artifactRepository.findArtifacts(group, name) } returns Result.success(artifacts)

            // Act
            val result = artifactService.getAllVersions(
                group = group,
                name = name
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe artifacts

            coVerify { artifactRepository.findArtifacts(group, name) }
        }
    }

    "getArtifact with version should return the specific version" {
        runTest {
            // Arrange
            val group = "org.example"
            val name = "test-artifact"
            val version = "1.0.0"
            val coordinates = ArtifactCoordinates(group = group, name = name, version = version)

            val artifact = Artifact(
                id = ArtifactId("art-123"),
                coordinates = coordinates,
                createdBy = userId,
                createdAt = Instant.now().minusSeconds(7200),
                merkleRoot = MerkleRootHash("hash1")
            )

            coEvery { artifactRepository.findByCoordinates(coordinates) } returns Result.success(artifact)

            // Act
            val result = artifactService.getArtifact(
                group = group,
                name = name,
                version = version
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe artifact

            coVerify { artifactRepository.findByCoordinates(coordinates) }
        }
    }

    "getArtifact without version should return the latest version" {
        runTest {
            // Arrange
            val group = "org.example"
            val name = "test-artifact"

            val artifacts = listOf(
                Artifact(
                    id = ArtifactId("art-123"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "1.0.0"),
                    createdBy = userId,
                    createdAt = Instant.now().minusSeconds(7200),
                    merkleRoot = MerkleRootHash("hash1")
                ),
                Artifact(
                    id = ArtifactId("art-456"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "2.0.0"),
                    createdBy = userId,
                    createdAt = Instant.now(), // El más reciente
                    merkleRoot = MerkleRootHash("hash2")
                ),
                Artifact(
                    id = ArtifactId("art-789"),
                    coordinates = ArtifactCoordinates(group = group, name = name, version = "1.5.0"),
                    createdBy = userId,
                    createdAt = Instant.now().minusSeconds(3600),
                    merkleRoot = MerkleRootHash("hash3")
                )
            )

            val latestArtifact = artifacts[1] // 2.0.0 es el más reciente

            coEvery { artifactRepository.findArtifacts(group, name) } returns Result.success(artifacts)

            // Act
            val result = artifactService.getArtifact(
                group = group,
                name = name,
                version = null
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe latestArtifact

            coVerify { artifactRepository.findArtifacts(group, name) }
        }
    }

    "getArtifact should handle empty result when no artifacts exist" {
        runTest {
            // Arrange
            val group = "non.existent"
            val name = "no-artifact"

            coEvery { artifactRepository.findArtifacts(group, name) } returns Result.success(emptyList())

            // Act
            val result = artifactService.getArtifact(
                group = group,
                name = name,
                version = null
            )

            // Assert
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe null

            coVerify { artifactRepository.findArtifacts(group, name) }
        }
    }

    "deleteArtifact should handle repository failure gracefully" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("art-123")
            val expectedException = RuntimeException("Database connection failed")

            coEvery { artifactRepository.findById(artifactId) } returns Result.failure(expectedException)

            // Act
            val result = artifactService.deleteArtifact(
                artifactId = artifactId,
                deletedBy = userId
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe expectedException

            coVerify { artifactRepository.findById(artifactId) }
            coVerify(exactly = 0) { artifactRepository.deleteById(any()) }
            verify(exactly = 0) { publishDeleteEvent(any()) }
        }
    }

    "publishArtifact should handle repository save failure" {
        runTest {
            // Arrange
            val coordinates = ArtifactCoordinates(group = "org.example", name = "test-artifact", version = "1.0.0")
            val merkleRootHash = "abcdef1234567890abcdef1234567890"
            val expectedException = RuntimeException("Failed to save artifact")

            coEvery { registryRepository.findById(registryId) } returns Result.success(testRepo)
            coEvery { registryRepository.isRepositoryActive(registryId) } returns true
            coEvery { artifactRepository.findByCoordinates(coordinates) } returns Result.success(null)
            coEvery { artifactRepository.save(any()) } returns Result.failure(expectedException)

            // Act
            val result = artifactService.publishArtifact(
                registryId = registryId,
                group = coordinates.group,
                name = coordinates.name,
                version = coordinates.version,
                createdBy = userId,
                merkleRoot = merkleRootHash
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe expectedException

            coVerify { artifactRepository.save(any()) }
            verify(exactly = 0) { publishArtifactEvent(any()) }
        }
    }

    "downloadArtifact should handle repository failure" {
        runTest {
            // Arrange
            val artifactId = ArtifactId("art-123")
            val expectedException = RuntimeException("Database error")

            coEvery { artifactRepository.findById(artifactId) } returns Result.failure(expectedException)

            // Act
            val result = artifactService.downloadArtifact(
                artifactId = artifactId,
                downloadedBy = userId
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe expectedException

            coVerify { artifactRepository.findById(artifactId) }
            verify(exactly = 0) { publishDownloadEvent(any()) }
        }
    }

    "getAllVersions should handle repository failure" {
        runTest {
            // Arrange
            val group = "org.example"
            val name = "test-artifact"
            val expectedException = RuntimeException("Failed to retrieve artifacts")

            coEvery { artifactRepository.findArtifacts(group, name) } returns Result.failure(expectedException)

            // Act
            val result = artifactService.getAllVersions(
                group = group,
                name = name
            )

            // Assert
            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe expectedException

            coVerify { artifactRepository.findArtifacts(group, name) }
        }
    }
})