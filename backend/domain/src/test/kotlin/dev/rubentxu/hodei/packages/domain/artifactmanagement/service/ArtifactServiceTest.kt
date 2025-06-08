package dev.rubentxu.hodei.packages.domain.artifactmanagement.service

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
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
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
    // lateinit var mockFormatHandler: FormatHandler // Eliminada, usaremos handlers específicos del mapa
    lateinit var mavenFormatHandler: FormatHandler
    lateinit var npmFormatHandler: FormatHandler
    lateinit var pypiFormatHandler: FormatHandler
    lateinit var dockerFormatHandler: FormatHandler
    lateinit var handlers: Map<ArtifactType, FormatHandler>
    lateinit var mockDependencies: List<ArtifactDependency> // Declaración movida aquí

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

    // Este Map simula lo que el usuario podría pasar en el comando
    val testProvidedUserMetadata: Map<String, String>? = mapOf(
        "description" to "User provided description",
        "user_tag" to "custom-user-tag",
        "user_license" to "Apache-2.0-User"
    )

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
        description = "Base test metadata", // Este es un valor base, será sobreescrito o complementado
        sizeInBytes = testContent.size.toLong()
    )

    // testProvidedUserMetadata se define arriba

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
        // mockFormatHandler = mockk() // Eliminada

        mavenFormatHandler = mockk()
        npmFormatHandler = mockk()
        pypiFormatHandler = mockk()
        dockerFormatHandler = mockk()

        handlers = mapOf(
            ArtifactType.MAVEN to mavenFormatHandler,
            ArtifactType.NPM to npmFormatHandler,
            ArtifactType.PYPI to pypiFormatHandler,
            ArtifactType.DOCKER to dockerFormatHandler
        )

        publishArtifactEvent = mockk(relaxed = true)
        publishDownloadEvent = mockk(relaxed = true)
        publishMetadataUpdateEvent = mockk(relaxed = true)
        publishDeleteEvent = mockk(relaxed = true)

        artifactService = ArtifactService(
            artifactRepository = artifactRepository,
            registryRepository = registryRepository,
            storageService = storageService,
            handlers = handlers, // Usar el mapa completo de handlers
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
        // Updated to reflect Map<String, String>? for providedMetadata in extractCoordinates
        // Como testArtifactType es MAVEN, configuramos mavenFormatHandler
        coEvery { mavenFormatHandler.extractCoordinates(testFilename, testContent, testProvidedUserMetadata) } returns Result.success(Pair(baseCoordinates, MetadataSource.FILENAME_INFERRED))

        // Mock for extractMetadataWithSources
        // Actualizar el mock para extractMetadataWithSources para mavenFormatHandler
        // Debe usar el artifactId que se le pasa como argumento
        coEvery { mavenFormatHandler.extractMetadataWithSources(eq(testFilename), eq(testContent), any(), any(), any()) } answers {
            val actualArtifactIdArg = args[3] // This could be ArtifactId or String
            val artifactIdForMetadata = when (actualArtifactIdArg) {
                is ArtifactId -> actualArtifactIdArg
                is String -> ArtifactId(actualArtifactIdArg)
                else -> throw ClassCastException("Argument for artifactId was neither ArtifactId nor String: ${actualArtifactIdArg?.javaClass?.name}")
            }
            Result.success(
                ArtifactMetadataWithSources(
                    id = artifactIdForMetadata, // Use the correctly typed ArtifactId
                    createdBy = MetadataWithSource(testUserId, MetadataSource.SYSTEM_GENERATED),
                    createdAt = MetadataWithSource(Instant.now(), MetadataSource.SYSTEM_GENERATED),
                    description = MetadataWithSource("Mocked description from handler", MetadataSource.CONTENT_EXTRACTED),
                    licenses = MetadataWithSource(listOf("MIT-Handler"), MetadataSource.CONTENT_EXTRACTED),
                    homepageUrl = MetadataWithSource("http://handler.example.com", MetadataSource.CONTENT_EXTRACTED),
                    repositoryUrl = MetadataWithSource("http://github.com/handler/repo", MetadataSource.CONTENT_EXTRACTED),
                    sizeInBytes = MetadataWithSource(testContent.size.toLong() + 10, MetadataSource.CONTENT_EXTRACTED),
                    checksums = MetadataWithSource(mapOf("SHA-1" to "handler-sha1"), MetadataSource.CONTENT_EXTRACTED),
                    additionalMetadata = mapOf(
                        "tags" to MetadataWithSource(listOf("handler_tag", "processed_tag"), MetadataSource.CONTENT_EXTRACTED),
                        "customHandlerField" to MetadataWithSource("handler_value", MetadataSource.CONTENT_EXTRACTED)
                    )
                )
            )
        }

        // Updated to determinePackagingType para mavenFormatHandler
        coEvery { mavenFormatHandler.determinePackagingType(testFilename, testContent) } returns Result.success(Pair("application/java-archive", MetadataSource.CONTENT_EXTRACTED))

        // Mock for extractDependencies para mavenFormatHandler
        mockDependencies = listOf( // Inicialización de la variable de clase
            ArtifactDependency(
                coordinates = ArtifactCoordinates(name = "dep1", version = ArtifactVersion("1.0")),
                scope = "compile"
            )
        )
        coEvery { mavenFormatHandler.extractDependencies(testContent) } returns Result.success(mockDependencies)
    }

    // --- uploadArtifact Tests ---
    "uploadArtifact should successfully create and save a new artifact" {
        runTest {
            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                providedUserMetadata = testProvidedUserMetadata,
                uploaderUserId = testUserId
            )

            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(null)
            coEvery { artifactRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = artifactService.uploadArtifact(command)

            result.shouldBeSuccess { artifact ->
                artifact.coordinates shouldBe baseCoordinates
                artifact.contentHash shouldBe testContentHash
                artifact.metadata.createdBy shouldBe testUserId // Viene de ArtifactMetadataWithSources.createdBy.value
                artifact.metadata.description shouldBe "Mocked description from handler" // Viene de ArtifactMetadataWithSources
                artifact.sizeInBytes shouldBe testContent.size.toLong() // Es el tamaño real del contenido
                artifact.tags shouldBe listOf("handler_tag", "processed_tag") // De additionalMetadata
                artifact.packagingType shouldBe "application/java-archive"
                artifact.dependencies shouldBe mockDependencies
            }

            coVerify { registryRepository.findById(command.registryId) }
            coVerify { registryRepository.isRepositoryActive(command.registryId) }
            // Verificar llamadas a mavenFormatHandler ya que command.artifactType es MAVEN
            coVerify { mavenFormatHandler.extractCoordinates(command.filename, command.content, command.providedUserMetadata) }
            coVerify { mavenFormatHandler.extractMetadataWithSources(command.filename, command.content, command.providedUserMetadata, any(), command.uploaderUserId) }
            coVerify { mavenFormatHandler.determinePackagingType(command.filename, command.content) }
            coVerify { mavenFormatHandler.extractDependencies(command.content) }

            coVerify { storageService.calculateHash(command.content) }
            coVerify { artifactRepository.findByCoordinates(baseCoordinates) }
            coVerify { storageService.store(command.content) }

            val artifactSlot = slot<Artifact>()
            coVerify { artifactRepository.save(capture(artifactSlot)) }
            artifactSlot.captured.id shouldBe artifactSlot.captured.metadata.id

            val eventSlot = slot<ArtifactPublishedEvent>()
            verify { publishArtifactEvent(capture(eventSlot)) }
            eventSlot.captured.publishedBy shouldBe command.uploaderUserId
            eventSlot.captured.artifactId shouldBe artifactSlot.captured.id
        }
    }

    "uploadArtifact should return failure when repository doesn't exist" {
        runTest {
            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
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
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
            )
            coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns false


            val result = artifactService.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "Registry '${testRegistry.name}' (ID: ${testRegistryId}) is not active"
            }
        }
    }

    "uploadArtifact should return failure when FormatHandler is not found for artifactType" {
        runTest {
            val unknownArtifactType = ArtifactType.PYPI // Este tipo se eliminará del mapa de handlers para la prueba
            val genericRegistry = testRegistry.copy(format = ArtifactType.GENERIC) // El formato del repo no impide el tipo de artefacto aquí

            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(genericRegistry)
            coEvery { registryRepository.isRepositoryActive(testRegistryId) } returns true


            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = unknownArtifactType, // Intentar subir un tipo para el cual el handler será removido
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
            )

            val currentHandlersMinusUnknown = handlers.toMutableMap()
            currentHandlersMinusUnknown.remove(unknownArtifactType) // Remover el handler para PYPI

            val serviceWithMissingHandler = ArtifactService( // Crear nueva instancia con handlers modificados
                artifactRepository = artifactRepository,
                registryRepository = registryRepository,
                storageService = storageService,
                handlers = currentHandlersMinusUnknown, // Usar el mapa modificado
                publishArtifactEvent = publishArtifactEvent,
                publishDownloadEvent = publishDownloadEvent,
                publishMetadataUpdateEvent = publishMetadataUpdateEvent,
                publishDeleteEvent = publishDeleteEvent
            )

            val result = serviceWithMissingHandler.uploadArtifact(command)

            result.shouldBeFailure {
                it.message shouldContain "ArtifactType '$unknownArtifactType' not supported"
            }
        }
    }

    "uploadArtifact should return failure when artifact already exists" {
        runTest {
            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
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
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
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
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
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
                storageConfig = StorageConfig(path = "/tmp/groupmeta",blobStoreName = "minio"),
                groupConfig = GroupConfig(members = emptyList())
            )
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(groupRegistry)

            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
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
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = testArtifactType,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Repository '${readOnlyRegistry.name}' is read-only"
            }
        }
    }

    "uploadArtifact should fail if artifactType is incompatible with repository format" {
        runTest {
            val mavenRegistry = testRegistry.copy(format = ArtifactType.MAVEN)
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(mavenRegistry)

            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = ArtifactType.NPM,
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
            )
            val result = artifactService.uploadArtifact(command)
            result.shouldBeFailure {
                it.message shouldContain "Artifact type 'NPM' is not allowed in repository '${mavenRegistry.name}' which is of format 'MAVEN'"
            }
        }
    }

    "uploadArtifact should succeed if repository format is GENERIC and artifactType is specific" {
        runTest {
            val genericRegistry = testRegistry.copy(format = ArtifactType.GENERIC)
            coEvery { registryRepository.findById(testRegistryId) } returns Result.success(genericRegistry)
            coEvery { artifactRepository.findByCoordinates(baseCoordinates) } returns Result.success(null)
            coEvery { artifactRepository.save(any()) } answers { Result.success(firstArg()) }


            val command = UploadArtifactCommand(
                registryId = testRegistryId,
                content = testContent,
                filename = testFilename,
                artifactType = ArtifactType.MAVEN, // El artefacto es MAVEN
                uploaderUserId = testUserId,
                providedUserMetadata = testProvidedUserMetadata
            )

            // No se necesita un nuevo servicio. El artifactService global ya tiene mavenFormatHandler.
            // Las configuraciones para mavenFormatHandler ya están en beforeTest o se pueden añadir aquí si son específicas.

            // Ejemplo de configuración específica si fuera necesaria (normalmente ya está en beforeTest):
            // coEvery { mavenFormatHandler.extractCoordinates(command.filename, command.content, command.providedUserMetadata) } returns Result.success(Pair(baseCoordinates, MetadataSource.FILENAME_INFERRED))
            // ... y así para los otros métodos de mavenFormatHandler ...


            val result = artifactService.uploadArtifact(command) // Usar el artifactService global
            result.shouldBeSuccess()

            // Verificar que se llamó a los métodos de mavenFormatHandler
            coVerify { mavenFormatHandler.extractCoordinates(command.filename, command.content, command.providedUserMetadata) }
            coVerify { mavenFormatHandler.extractMetadataWithSources(command.filename, command.content, command.providedUserMetadata, any(), command.uploaderUserId) }
            coVerify { mavenFormatHandler.determinePackagingType(command.filename, command.content) }
            coVerify { mavenFormatHandler.extractDependencies(command.content) }
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
            // testArtifactType es MAVEN, así que se usará mavenFormatHandler
            coEvery { mavenFormatHandler.generateDescriptor(artifactToDescribe) } returns Result.success(
                expectedDescriptor
            )

            val result = artifactService.generateArtifactDescriptor(testArtifactId, testArtifactType)

            result.shouldBeSuccess { it shouldBe expectedDescriptor }
            coVerify { mavenFormatHandler.generateDescriptor(artifactToDescribe) }
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
            val typeWithoutHandler = ArtifactType.NPM // Escoger un tipo para el cual removeremos el handler

            val handlersForThisTest = handlers.toMutableMap()
            handlersForThisTest.remove(typeWithoutHandler)

            val serviceWithModifiedHandlers = ArtifactService(
                artifactRepository, registryRepository, storageService, handlersForThisTest,
                publishArtifactEvent, publishDownloadEvent, publishMetadataUpdateEvent, publishDeleteEvent
            )

            val result = serviceWithModifiedHandlers.generateArtifactDescriptor(testArtifactId, typeWithoutHandler)

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
            // testArtifactType es MAVEN, así que se usará mavenFormatHandler
            coEvery { mavenFormatHandler.generateDescriptor(artifactToDescribe) } returns Result.failure(
                RuntimeException(
                    "Generation failed"
                )
            )

            val result = artifactService.generateArtifactDescriptor(testArtifactId, testArtifactType)

            result.shouldBeFailure {
                it.message shouldBe "Generation failed"
            }
            coVerify { mavenFormatHandler.generateDescriptor(artifactToDescribe) }
        }
    }
})