package dev.rubentxu.hodei.packages.domain.artifacmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.command.UploadArtifactCommand
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.service.ArtifactService
// Importa la clase ArtifactService del paquete correcto si es necesario
// import dev.rubentxu.hodei.packages.domain.artifactmanagement.service.ArtifactService
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.StorageService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.*

class ArtifactServiceTest : StringSpec({

    // --- Mocks & Setup ---
    lateinit var artifactRepository: ArtifactRepository
    lateinit var registryRepository: RegistryRepository
    lateinit var storageService: StorageService
    lateinit var mockFormatHandler: FormatHandler
    lateinit var handlers: Map<ArtifactType, FormatHandler>

    lateinit var publishArtifactEvent: (ArtifactPublishedEvent) -> Unit
    lateinit var publishDownloadEvent: (ArtifactDownloadedEvent) -> Unit
    lateinit var publishMetadataUpdateEvent: (ArtifactMetadataUpdatedEvent) -> Unit
    lateinit var publishDeleteEvent: (ArtifactDeletedEvent) -> Unit
    lateinit var artifactService: ArtifactService

    // --- Common Test Data ---
    val testRegistryId = RegistryId(UUID.randomUUID())
    val testUserId = UserId("user-test-123")
    val testArtifactId = ArtifactId("art-test-123")
    val testContentHash = ContentHash("sha256-dummyhashvaluefortest")
    val testFilename = "test-artifact.jar"
    val testContent = "dummy content".toByteArray()
    val testArtifactType = ArtifactType.MAVEN // Usado para el formato del repositorio y del artefacto

    // Actualizado para usar HostedRegistry según el nuevo modelo de datos
    val testRegistry = HostedRegistry(
        id = testRegistryId,
        name = "test-repo",
        description = "Test repository",
        format = testArtifactType, // El formato del HostedRegistry
        deploymentPolicy = DeploymentPolicy.ALLOW_REDEPLOY_STABLE,
        storageConfig = StorageConfig( storageType = StorageType.LOCAL, path = "/tmp/repo", blobStoreName = "local"), // Asumiendo que StorageConfig y StorageType están definidos
        online = true // HostedRegistry tiene 'online' con valor por defecto true
        // Campos como createdBy, createdAt, updatedAt, isPublic no son parte de la interfaz Registry
        // ni de HostedRegistry directamente. Se gestionarían a un nivel superior o en la capa de persistencia.
    )

    val baseArtifactMetadata = ArtifactMetadata(
        id = testArtifactId,
        createdBy = testUserId,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        description = "Base test metadata",
        sizeInBytes = testContent.size.toLong()
    )

    val baseCoordinates = ArtifactCoordinates(
        group = ArtifactGroup("com.example"),
        name = "test-artifact",
        version = ArtifactVersion("1.0.0"),
        extension = ArtifactExtension("jar")
    )

    beforeTest {
        artifactRepository = mockk()
        registryRepository = mockk()
        storageService = mockk()
        mockFormatHandler = mockk()
        handlers = mapOf(testArtifactType to mockFormatHandler)

        publishArtifactEvent = mockk(relaxed = true)
        publishDownloadEvent = mockk(relaxed = true)
        publishMetadataUpdateEvent = mockk(relaxed = true)
        publishDeleteEvent = mockk(relaxed = true)

        artifactService = ArtifactService(
            artifactRepository = artifactRepository,
            registryRepository = registryRepository,
            storageService = storageService,
            handlers = handlers,
            publishArtifactEvent = publishArtifactEvent,
            publishDownloadEvent = publishDownloadEvent,
            publishMetadataUpdateEvent = publishMetadataUpdateEvent,
            publishDeleteEvent = publishDeleteEvent
        )

        // Common successful mock setups
        coEvery { registryRepository.findById(testRegistryId) } returns Result.success(testRegistry)
        coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns true // Asume que testRegistry.online es true
        coEvery { storageService.calculateHash(any()) } returns testContentHash
        coEvery { storageService.store(any()) } returns Result.success(ContentHash("sha256-dummyhashvaluefortest"))
        coEvery { mockFormatHandler.parseCoordinates(any(), any()) } returns Result.success(baseCoordinates)
        coEvery { mockFormatHandler.parseMetadata(any()) } returns Result.success(mapOf("handler_key" to "handler_value", "tags" to "handler_value")) // Añadido "tags" para el test
        coEvery { mockFormatHandler.getPackagingType(any(), any()) } returns Result.success("application/java-archive")
    }

    // --- uploadArtifact Tests ---
    "uploadArtifact should successfully create and save a new artifact" {
        runTest {
            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                createdBy = testUserId,
                providedMetadata = baseArtifactMetadata.copy(description = "Provided by command")
            )

            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(null)
            coEvery { artifactRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = artifactService.uploadArtifact(command)

            result.shouldBeSuccess { artifact ->
                artifact.coordinates shouldBe baseCoordinates
                artifact.contentHash shouldBe testContentHash
                artifact.metadata.createdBy shouldBe command.createdBy
                artifact.metadata.description shouldBe "Provided by command" // From command.providedMetadata
                artifact.metadata.sizeInBytes shouldBe testContent.size.toLong() // Overridden by service
                artifact.tags shouldBe listOf("handler_value") // Asumiendo que "tags" viene del handlerParsedRawMetadata
                artifact.packagingType shouldBe "application/java-archive"
            }

            coVerify { registryRepository.findById(command.registryId) }
            coVerify { registryRepository.isRepositoryActive(command.registryId) }
            coVerify { mockFormatHandler.parseCoordinates(command.filename, command.content) }
            coVerify { mockFormatHandler.parseMetadata(command.content) }
            coVerify { mockFormatHandler.getPackagingType(command.filename, command.content) }
            coVerify { storageService.calculateHash(command.content) }
            coVerify { artifactRepository.findByCoordinates(baseCoordinates) }
            coVerify { storageService.store(command.content) }

            val artifactSlot = slot<Artifact>()
            coVerify { artifactRepository.save(capture(artifactSlot)) }
            artifactSlot.captured.id shouldBe artifactSlot.captured.metadata.id // Ensure IDs match

            val eventSlot = slot<ArtifactPublishedEvent>()
            verify { publishArtifactEvent(capture(eventSlot)) }
            eventSlot.captured.publishedBy shouldBe command.createdBy
            eventSlot.captured.artifactId shouldBe artifactSlot.captured.id
        }
    }

    "uploadArtifact should return failure when repository doesn't exist" {
        runTest {
            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            coEvery { registryRepository.findById(testRegistryId) } returns Result.failure(
                RuntimeException("Registry with ID '${testRegistryId}' not found")
            )

            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "Registry with ID '${testRegistryId}' not found"
            }
            coVerify(exactly = 0) { artifactRepository.save(any()) }
        }
    }

    "uploadArtifact should return failure when repository is not active" {
        runTest {
            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            // Simula que el repositorio no está activo, aunque testRegistry.online sea true,
            // el mock de isRepositoryActive tiene precedencia.
            coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns false


            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "Registry '${testRegistry.name}' (ID: ${testRegistryId}) is not active"
            }
        }
    }

    "uploadArtifact should return failure when FormatHandler is not found for artifactType" {
        runTest {
            val unknownArtifactType = ArtifactType.PYPI // Assuming PYPI is not in `handlers`
            val genericRegistry = testRegistry.copy(format = ArtifactType.GENERIC) // Use a GENERIC registry

            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(genericRegistry)
            // Ensure isRepositoryActive is also mocked for this specific registry instance if it's checked after findById
            coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns true


            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                unknownArtifactType,
                testUserId,
                baseArtifactMetadata
            )

            // Ensure 'handlers' does not contain unknownArtifactType
            val currentHandlers = handlers.toMutableMap()
            currentHandlers.remove(unknownArtifactType)
            // Re-initialize service with potentially modified handlers if necessary, or ensure initial setup is correct
            // For this test, we assume the initial 'handlers' map passed to the service constructor
            // already lacks a handler for 'unknownArtifactType'. If 'handlers' is a field and modified,
            // the service instance might need to be recreated or the field updated.
            // Given the setup, 'handlers' is passed in constructor, so it's fixed for the service instance.
            // We just need to ensure our test setup for 'handlers' reflects the absence of PYPI.
            // The default 'handlers' map in beforeTest only contains testArtifactType (MAVEN).

            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "ArtifactType '$unknownArtifactType' not supported"
            }
        }
    }

    "uploadArtifact should return failure when artifact already exists" {
        runTest {
            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            val existingArtifact = mockk<Artifact>()
            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(existingArtifact)

            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "already exists"
            }
        }
    }

    "uploadArtifact should return failure when storageService store fails" {
        runTest {
            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(null)
            coEvery { storageService.store(testContent) } returns Result.failure(RuntimeException("Disk full"))

            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "Failed to store artifact content"
                it.cause?.message shouldBe "Disk full"
            }
        }
    }

    "uploadArtifact should fail if repository type is PROXY" {
        runTest {
            val proxyRegistry = ProxyRegistry(
                id = testRegistryId,
                name = "proxy-repo",
                format = testArtifactType,
                storageConfig = StorageConfig(path = "/tmp/proxycache",blobStoreName = "minio"),
                proxyConfig = ProxyConfig(remoteUrl = "http://remote.example.com")
            )
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(proxyRegistry)

            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Direct upload not allowed to PROXY repositories"
            }
        }
    }

    "uploadArtifact should fail if repository type is GROUP" {
        runTest {
            val groupRegistry = GroupRegistry(
                id = testRegistryId,
                name = "group-repo",
                format = testArtifactType,
                storageConfig = StorageConfig(path = "/tmp/groupmeta",blobStoreName = "minio"), // Storage for group's own metadata
                groupConfig = GroupConfig(members = emptyList())
            )
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(groupRegistry)

            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Direct upload not allowed to GROUP repositories"
            }
        }
    }

    "uploadArtifact should fail if hosted repository is READ_ONLY" {
        runTest {
            val readOnlyRegistry = testRegistry.copy(deploymentPolicy = DeploymentPolicy.READ_ONLY)
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(readOnlyRegistry)

            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                testArtifactType,
                testUserId,
                baseArtifactMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Repository '${readOnlyRegistry.name}' is read-only"
            }
        }
    }

    "uploadArtifact should fail if artifactType is incompatible with repository format" {
        runTest {
            val mavenRegistry = testRegistry.copy(format = ArtifactType.MAVEN) // Registry is MAVEN
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(mavenRegistry)

            val command = UploadArtifactCommand(
                testRegistryId,
                testContent,
                testFilename,
                ArtifactType.NPM, // Attempting to upload an NPM artifact
                testUserId,
                baseArtifactMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Artifact type 'NPM' is not allowed in repository '${mavenRegistry.name}' which is of format 'MAVEN'"
            }
        }
    }

    "uploadArtifact should succeed if repository format is GENERIC and artifactType is specific" {
        runTest {
            // GENERIC repositories are often more permissive
            val genericRegistry = testRegistry.copy(format = ArtifactType.GENERIC)
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(genericRegistry)
            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(null) // No existing
            coEvery { artifactRepository.save(any()) } answers { Result.success(firstArg()) }


            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = ArtifactType.MAVEN, // Uploading a MAVEN artifact to a GENERIC repo
                createdBy = testUserId,
                providedMetadata = baseArtifactMetadata
            )

            // Need to ensure the MAVEN handler is available if we are testing with MAVEN artifactType
            val mavenHandler = mockk<FormatHandler>()
            handlers = mapOf(ArtifactType.MAVEN to mavenHandler) // Override handlers for this test
            artifactService = ArtifactService( // Re-init service with new handlers
                artifactRepository, registryRepository, storageService, handlers,
                publishArtifactEvent, publishDownloadEvent, publishMetadataUpdateEvent, publishDeleteEvent
            )
            coEvery { mavenHandler.parseCoordinates(any(), any()) } returns Result.success(baseCoordinates)
            coEvery { mavenHandler.parseMetadata(any()) } returns Result.success(mapOf("tags" to "maven_tag"))
            coEvery { mavenHandler.getPackagingType(any(), any()) } returns Result.success("application/java-archive")


            val result = artifactService.uploadArtifact(command)
            result.shouldBeSuccess()
        }
    }


    // --- updateArtifactMetadata Tests ---
    "updateArtifactMetadata should successfully update metadata and publish event" {
        runTest {
            val existingArtifact = Artifact(
                id = testArtifactId,
                contentHash = testContentHash,
                coordinates = baseCoordinates,
                metadata = baseArtifactMetadata.copy(description = "Old description")
            )
            val newMetadataValues = baseArtifactMetadata.copy(
                description = "Updated description",
                licenses = listOf("MIT"),
                // id, createdBy, createdAt will be ignored from newMetadataValues and preserved from existing
                id = ArtifactId("ignored-id"), // Should be ignored
                createdBy = UserId("ignored-user"), // Should be ignored
                createdAt = Instant.EPOCH // Should be ignored
            )

            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(existingArtifact)
            coEvery { artifactRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = artifactService.updateArtifactMetadata(testArtifactId, newMetadataValues, testUserId)

            result.shouldBeSuccess { updatedArtifact ->
                updatedArtifact.id shouldBe testArtifactId
                updatedArtifact.metadata.description shouldBe "Updated description"
                updatedArtifact.metadata.licenses shouldBe listOf("MIT")
                updatedArtifact.metadata.createdBy shouldBe existingArtifact.metadata.createdBy // Preserved
                updatedArtifact.metadata.createdAt shouldBe existingArtifact.metadata.createdAt // Preserved
                updatedArtifact.metadata.updatedAt shouldNotBe existingArtifact.metadata.updatedAt // Should be updated
            }

            val eventSlot = slot<ArtifactMetadataUpdatedEvent>()
            verify { publishMetadataUpdateEvent(capture(eventSlot)) }
            eventSlot.captured.artifactId shouldBe testArtifactId
            eventSlot.captured.updatedBy shouldBe testUserId
            eventSlot.captured.updatedMetadata["description"] shouldBe "Updated description"
        }
    }

    "updateArtifactMetadata should return failure if artifact not found" {
        runTest {
            val nonExistentArtifactId = ArtifactId("non-existent")
            val newMetadataValues = baseArtifactMetadata.copy(description = "Update attempt")

            coEvery { artifactRepository.findById(nonExistentArtifactId) } returns Result.success(null)

            val result = artifactService.updateArtifactMetadata(nonExistentArtifactId, newMetadataValues, testUserId)

            result.shouldBeFailure {
                it.message shouldContain "Artifact with ID '${nonExistentArtifactId.value}' not found"
            }
            coVerify(exactly = 0) { artifactRepository.save(any()) }
        }
    }

    // --- downloadArtifact Tests (adapted) ---
    "downloadArtifact should record download and publish event" {
        runTest {
            val clientIp = "192.168.1.100"
            val userAgent = "TestClient/1.0"
            val artifactToDownload =
                Artifact(testArtifactId, testContentHash, baseCoordinates, metadata = baseArtifactMetadata)

            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDownload)

            val result = artifactService.downloadArtifact(testArtifactId, testUserId, clientIp, userAgent)

            result.shouldBeSuccess { it shouldBe artifactToDownload }

            val eventSlot = slot<ArtifactDownloadedEvent>()
            verify { publishDownloadEvent(capture(eventSlot)) }
            with(eventSlot.captured) {
                artifactId shouldBe testArtifactId
                downloadedBy shouldBe testUserId
                this.clientIp shouldBe clientIp
                this.userAgent shouldBe userAgent
            }
        }
    }

    "downloadArtifact should return failure when artifact status is not ACTIVE or PRE_RELEASE" {
        runTest {
            val artifactToDownload = Artifact(
                testArtifactId,
                testContentHash,
                baseCoordinates,
                metadata = baseArtifactMetadata,
                status = ArtifactStatus.DEPRECATED // Not downloadable by default
            )
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDownload)

            val result = artifactService.downloadArtifact(testArtifactId, testUserId)

            result.shouldBeFailure {
                it.message shouldContain "not in an active/pre-release state"
            }
            verify(exactly = 0) { publishDownloadEvent(any()) }
        }
    }


    // --- retrieveArtifactContent Tests ---
    "retrieveArtifactContent should return content when successful" {
        runTest {
            coEvery { storageService.retrieve(testContentHash) } returns Result.success(testContent)

            val result = artifactService.retrieveArtifactContent(testRegistryId, testContentHash)

            result.shouldBeSuccess { it shouldBe testContent }
            coVerify { registryRepository.findById(testRegistryId) }
            coVerify { registryRepository.isRepositoryActive(testRegistryId) } // Verifies check for active repo
            coVerify { storageService.retrieve(testContentHash) }
        }
    }

    "retrieveArtifactContent should return failure if registry not found" {
        runTest {
            coEvery { registryRepository.findById(testRegistryId) } returns Result.failure(
                RuntimeException("Registry with ID '${testRegistryId}' not found")
            )

            val result = artifactService.retrieveArtifactContent(testRegistryId, testContentHash)
            result.shouldBeFailure {
                it.message shouldContain "Registry with ID '${testRegistryId}' not found"
            }
        }
    }

    "retrieveArtifactContent should return failure if registry is not active" {
        runTest {
            coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns false

            val result = artifactService.retrieveArtifactContent(testRegistryId, testContentHash)
            result.shouldBeFailure {
                it.message shouldContain "Registry '${testRegistry.name}' (ID: ${testRegistryId}) is not active"
            }
        }
    }


    "retrieveArtifactContent should return failure if storageService fails" {
        runTest {
            coEvery { storageService.retrieve(testContentHash) } returns Result.failure(RuntimeException("Storage error"))

            val result = artifactService.retrieveArtifactContent(testRegistryId, testContentHash)
            result.shouldBeFailure {
                it.message shouldContain "Failed to retrieve content"
                it.cause?.message shouldBe "Storage error"
            }
        }
    }

    // --- deleteArtifact Tests (adapted) ---
    "deleteArtifact should delete artifact and publish event" {
        runTest {
            val artifactToDelete =
                Artifact(testArtifactId, testContentHash, baseCoordinates, metadata = baseArtifactMetadata)
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDelete)
            coEvery { artifactRepository.deleteById(testArtifactId) } returns Result.success(true)

            val result = artifactService.deleteArtifact(testArtifactId, testUserId)

            result.shouldBeSuccess { it shouldBe true }
            coVerify { artifactRepository.deleteById(testArtifactId) }
            val eventSlot = slot<ArtifactDeletedEvent>()
            verify { publishDeleteEvent(capture(eventSlot)) }
            eventSlot.captured.artifactId shouldBe testArtifactId
            eventSlot.captured.deletedBy shouldBe testUserId
        }
    }

    "deleteArtifact should return success false if artifact not found for deletion" {
        runTest {
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(null)

            val result = artifactService.deleteArtifact(testArtifactId, testUserId)
            result.shouldBeSuccess { it shouldBe false } // Idempotent delete
            coVerify(exactly = 0) { artifactRepository.deleteById(any()) }
            verify(exactly = 0) { publishDeleteEvent(any()) }
        }
    }


    // --- getArtifact Tests (adapted for ArtifactCoordinates input) ---
    "getArtifact with specific coordinates should return the artifact" {
        runTest {
            val specificCoordinates = baseCoordinates.copy(version = ArtifactVersion("1.0.0-specific"))
            val expectedArtifact =
                Artifact(testArtifactId, testContentHash, specificCoordinates, metadata = baseArtifactMetadata)
            coEvery { artifactRepository.findByCoordinates(specificCoordinates) } returns Result.success(
                expectedArtifact
            )

            val result = artifactService.getArtifact(specificCoordinates)

            result.shouldBeSuccess { it shouldBe expectedArtifact }
            coVerify { artifactRepository.findByCoordinates(specificCoordinates) }
        }
    }

    "getArtifact should return null if no artifact matches coordinates" {
        runTest {
            val specificCoordinates = baseCoordinates.copy(name = "non-existent-artifact")
            coEvery { artifactRepository.findByCoordinates(specificCoordinates) } returns Result.success(null)

            val result = artifactService.getArtifact(specificCoordinates)

            result.shouldBeSuccess { it shouldBe null }
        }
    }


    // --- generateArtifactDescriptor Tests ---
    "generateArtifactDescriptor should return descriptor string when successful" {
        runTest {
            val artifactToDescribe =
                Artifact(testArtifactId, testContentHash, baseCoordinates, metadata = baseArtifactMetadata)
            val expectedDescriptor = "<xml>descriptor</xml>"
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDescribe)
            coEvery { mockFormatHandler.generateDescriptor(artifactToDescribe) } returns Result.success(
                expectedDescriptor
            )

            val result = artifactService.generateArtifactDescriptor(testArtifactId, testArtifactType)

            result.shouldBeSuccess { it shouldBe expectedDescriptor }
        }
    }

    "generateArtifactDescriptor should return failure if artifact not found" {
        runTest {
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(null)

            val result = artifactService.generateArtifactDescriptor(testArtifactId, testArtifactType)

            result.shouldBeFailure {
                it.message shouldContain "not found for descriptor generation"
            }
        }
    }

    "generateArtifactDescriptor should return failure if handler not found" {
        runTest {
            val artifactToDescribe =
                Artifact(testArtifactId, testContentHash, baseCoordinates, metadata = baseArtifactMetadata)
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDescribe)
            val unknownType = ArtifactType.PYPI // Assuming not in handlers map

            val result = artifactService.generateArtifactDescriptor(testArtifactId, unknownType)

            result.shouldBeFailure {
                it.message shouldContain "not supported for descriptor generation"
            }
        }
    }

    "generateArtifactDescriptor should return failure if handler fails to generate" {
        runTest {
            val artifactToDescribe =
                Artifact(testArtifactId, testContentHash, baseCoordinates, metadata = baseArtifactMetadata)
            coEvery { artifactRepository.findById(testArtifactId) } returns Result.success(artifactToDescribe)
            coEvery { mockFormatHandler.generateDescriptor(artifactToDescribe) } returns Result.failure(
                RuntimeException(
                    "Generation failed"
                )
            )

            val result = artifactService.generateArtifactDescriptor(testArtifactId, testArtifactType)

            result.shouldBeFailure {
                it.message shouldBe "Generation failed"
            }
        }
    }
})
