package dev.rubentxu.hodei.packages.domain.registrymanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.command.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.events.ArtifactRegistryEvent
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.util.*

class RegistryServiceTest : StringSpec({

    lateinit var registryRepository: RegistryRepository
    lateinit var eventPublisher: (ArtifactRegistryEvent) -> Unit
    lateinit var registryService: RegistryService

    val testUserId = UserId(UUID.randomUUID().toString())
    val defaultStorageConfig = StorageConfig(
        path = "/tmp/repo",
        blobStoreName = "default",
        strictContentTypeValidation = true,
        storageType = StorageType.LOCAL
    )
    // Helper to create a default valid online status for commands if not specified otherwise
    val defaultOnlineStatus = true


    beforeTest {
        registryRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        registryService = RegistryService(registryRepository, eventPublisher)
    }

    // --- Create Hosted Registry Tests ---
    "handle CreateHostedRegistryCommand should create and persist a new hosted repository" {
        runTest {
            val command = CreateHostedRegistryCommand(
                name = "test-hosted-repo",
                format = ArtifactType.MAVEN,
                description = "Test Hosted Repository",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                deploymentPolicy = DeploymentPolicy.ALLOW_REDEPLOY_SNAPSHOT,
                online = defaultOnlineStatus
            )

            coEvery { registryRepository.existsByName(command.name) } returns Result.success(false)
            coEvery { registryRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = registryService.handle(command)

            result.shouldBeSuccess { registry ->
                registry.shouldBeInstanceOf<HostedRegistry>()
                registry.name shouldBe command.name
                registry.format shouldBe command.format
                registry.description shouldBe command.description
                registry.storageConfig shouldBe command.storageConfig
                registry.deploymentPolicy shouldBe command.deploymentPolicy
                registry.online shouldBe command.online
            }

            coVerify { registryRepository.existsByName(command.name) }
            coVerify { registryRepository.save(any<HostedRegistry>()) }
            verify {
                eventPublisher(match {
                    it is ArtifactRegistryEvent.ArtifactRegistryCreated &&
                            it.name == command.name &&
                            it.type == command.format &&
                            it.createdBy == command.requestedBy
                })
            }
        }
    }

    "handle CreateHostedRegistryCommand should return failure when repository name already exists" {
        runTest {
            val command = CreateHostedRegistryCommand(
                name = "existing-repo",
                format = ArtifactType.NPM,
                description = "This should fail",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                online = defaultOnlineStatus
            )
            coEvery { registryRepository.existsByName(command.name) } returns Result.success(true)

            val result = registryService.handle(command)

            result.shouldBeFailure {
                it.shouldBeInstanceOf<IllegalStateException>()
                it.message shouldBe "An artifact registry with name '${command.name}' already exists"
            }
            coVerify(exactly = 0) { registryRepository.save(any()) }
        }
    }

    // In /home/rubentxu/Proyectos/Kotlin/hodei-packages/backend/domain/src/test/kotlin/dev/rubentxu/hodei/packages/domain/registrymanagement/service/RegistryServiceTest.kt

    "handle CreateHostedRegistryCommand should return failure when existsByName from repository fails" {
        runTest {
            val command = CreateHostedRegistryCommand(
                name = "check-fails-repo",
                format = ArtifactType.NPM,
                description = "This should fail due to repo error",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                online = defaultOnlineStatus
            )
            val dbError = RuntimeException("DB connection failed")
            // This mock simulates the repository itself returning a failure Result
            coEvery { registryRepository.existsByName(command.name) } returns Result.failure(dbError)

            val result = registryService.handle(command)

            result.shouldBeFailure {
                // The exception 'it' in shouldBeFailure is the one directly from the Result.failure.
                // In createRegistryInternal, if existsByName returns a failure, that Result is propagated
                // by flatMap. The outer try-catch is NOT hit for this specific path.
                // Thus, 'it' is the original dbError.
                it shouldBe dbError
            }
            coVerify(exactly = 0) { registryRepository.save(any()) }
        }
    }

    "handle CreateHostedRegistryCommand should return failure when save from repository fails" {
        runTest {
            val command = CreateHostedRegistryCommand(
                name = "save-fails-repo",
                format = ArtifactType.MAVEN,
                description = "This should fail during save",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                online = defaultOnlineStatus
            )
            val saveError = RuntimeException("Disk full")
            coEvery { registryRepository.existsByName(command.name) } returns Result.success(false)
            // This mock simulates the repository save operation returning a failure Result
            coEvery { registryRepository.save(any()) } returns Result.failure(saveError)

            val result = registryService.handle(command)

            result.shouldBeFailure {
                // When registryRepository.save returns a Result.failure,
                // the flatMap in createRegistryInternal propagates this Result.failure directly.
                // The outer try-catch in createRegistryInternal is NOT hit for this specific path.
                // Thus, 'it' is the original saveError.
                it shouldBe saveError
            }
        }
    }


    // --- Create Proxy Registry Tests ---
    "handle CreateProxyRegistryCommand should create and persist a new proxy repository" {
        runTest {
            val command = CreateProxyRegistryCommand(
                name = "test-proxy-repo",
                format = ArtifactType.NPM,
                description = "Test Proxy Repository",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                proxyConfig = ProxyConfig(remoteUrl = "https://registry.npmjs.org/"),
                online = defaultOnlineStatus
            )

            coEvery { registryRepository.existsByName(command.name) } returns Result.success(false)
            coEvery { registryRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = registryService.handle(command)

            result.shouldBeSuccess { registry ->
                registry.shouldBeInstanceOf<ProxyRegistry>()
                registry.name shouldBe command.name
                registry.proxyConfig shouldBe command.proxyConfig
            }
            coVerify { registryRepository.save(any<ProxyRegistry>()) }
            verify { eventPublisher(any<ArtifactRegistryEvent.ArtifactRegistryCreated>()) }
        }
    }

    // --- Create Group Registry Tests ---
    "handle CreateGroupRegistryCommand should create and persist a new group repository" {
        runTest {
            val memberId = RegistryId.random()
            val command = CreateGroupRegistryCommand(
                name = "test-group-repo",
                format = ArtifactType.MAVEN,
                description = "Test Group Repository",
                requestedBy = testUserId,
                storageConfig = defaultStorageConfig,
                groupConfig = GroupConfig(members = listOf(memberId)),
                online = defaultOnlineStatus
            )

            coEvery { registryRepository.existsByName(command.name) } returns Result.success(false)
            coEvery { registryRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = registryService.handle(command)

            result.shouldBeSuccess { registry ->
                registry.shouldBeInstanceOf<GroupRegistry>()
                registry.name shouldBe command.name
                registry.groupConfig.members shouldBe listOf(memberId)
            }
            coVerify { registryRepository.save(any<GroupRegistry>()) }
            verify { eventPublisher(any<ArtifactRegistryEvent.ArtifactRegistryCreated>()) }
        }
    }


    // --- Update Registry Tests ---
    "handle UpdateRegistryCommand should update and persist changes for a HostedRegistry" {
        runTest {
            val repoId = RegistryId.random()
            val existingRepo = HostedRegistry(
                id = repoId,
                name = "test-repo-to-update",
                format = ArtifactType.MAVEN,
                description = "Original description",
                storageConfig = defaultStorageConfig,
                online = true,
                deploymentPolicy = DeploymentPolicy.ALLOW_REDEPLOY_SNAPSHOT
            )
            val command = UpdateRegistryCommand(
                registryId = repoId,
                description = "Updated description",
                online = false,
                storageConfig = defaultStorageConfig.copy(path = "/new/path"),
                cleanupPolicy = CleanupPolicy(maxVersionsToKeep = 10),
                deploymentPolicy = DeploymentPolicy.DISABLE_REDEPLOY,
                specificFormatConfig = mapOf("newKey" to "newValue"),
                requestedBy = testUserId
            )

            coEvery { registryRepository.findById(repoId) } returns Result.success(existingRepo)
            coEvery { registryRepository.save(any()) } answers { Result.success(firstArg()) }

            val result = registryService.handle(command)

            result.shouldBeSuccess { updatedRegistry ->
                updatedRegistry.shouldBeInstanceOf<HostedRegistry>()
                updatedRegistry.id shouldBe repoId
                updatedRegistry.description shouldBe command.description
                updatedRegistry.online shouldBe command.online
                updatedRegistry.storageConfig shouldBe command.storageConfig
                updatedRegistry.cleanupPolicy shouldBe command.cleanupPolicy
                (updatedRegistry as HostedRegistry).deploymentPolicy shouldBe command.deploymentPolicy
                (updatedRegistry as HostedRegistry).specificFormatConfig shouldBe command.specificFormatConfig
            }

            coVerify { registryRepository.findById(repoId) }
            coVerify { registryRepository.save(any<HostedRegistry>()) }
            verify {
                eventPublisher(match {
                    it is ArtifactRegistryEvent.ArtifactRegistryUpdated &&
                            it.registryId == repoId.value &&
                            it.updatedBy == command.requestedBy &&
                            it.changes.containsKey("description") &&
                            it.changes.containsKey("online") &&
                            it.changes.containsKey("storageConfig") &&
                            it.changes.containsKey("cleanupPolicy") &&
                            it.changes.containsKey("deploymentPolicy") &&
                            it.changes.containsKey("specificFormatConfig")
                })
            }
        }
    }

    "handle UpdateRegistryCommand should return failure when repository to update doesn't exist" {
        runTest {
            val repoId = RegistryId.random()
            val command = UpdateRegistryCommand(registryId = repoId, description = "New desc", requestedBy = testUserId)
            coEvery { registryRepository.findById(repoId) } returns Result.success(null)

            val result = registryService.handle(command)

            result.shouldBeFailure {
                it.shouldBeInstanceOf<IllegalArgumentException>()
                it.message shouldBe "ArtifactRegistry with ID '${repoId}' not found"
            }
            coVerify(exactly = 0) { registryRepository.save(any()) }
        }
    }

    "handle UpdateRegistryCommand should not publish event if no effective changes were made" {
        runTest {
            val repoId = RegistryId.random()
            val existingRepo = HostedRegistry(
                id = repoId, name = "no-change-repo", format = ArtifactType.GENERIC,
                description = "Original", storageConfig = defaultStorageConfig, online = true
            )
            val command = UpdateRegistryCommand(
                registryId = repoId,
                description = existingRepo.description, // Same
                online = existingRepo.online,           // Same
                storageConfig = existingRepo.storageConfig, //Same
                requestedBy = testUserId
            )

            coEvery { registryRepository.findById(repoId) } returns Result.success(existingRepo)
            coEvery { registryRepository.save(any()) } answers { Result.success(firstArg()) } // Save might be called

            val result = registryService.handle(command)
            result.shouldBeSuccess()

            verify(exactly = 0) { eventPublisher(any()) } // No event
        }
    }

    // --- Delete Registry Tests ---
    "handle DeleteRegistryCommand should delete and emit event when repository exists" {
        runTest {
            val repoId = RegistryId.random()
            val existingRepo = HostedRegistry(
                id = repoId, name = "to-be-deleted", format = ArtifactType.NPM,
                description = "Delete me", storageConfig = defaultStorageConfig
            )
            val command = DeleteRegistryCommand(registryId = repoId, requestedBy = testUserId)

            coEvery { registryRepository.findById(repoId) } returns Result.success(existingRepo)
            coEvery { registryRepository.deleteById(repoId) } returns Result.success(true)

            val result = registryService.handle(command)

            result.shouldBeSuccess { it shouldBe true }
            coVerify { registryRepository.deleteById(repoId) }
            verify {
                eventPublisher(match {
                    it is ArtifactRegistryEvent.ArtifactRegistryDeleted &&
                            it.registryId == repoId.value &&
                            it.name == existingRepo.name &&
                            it.deletedBy == command.requestedBy
                })
            }
        }
    }

    "handle DeleteRegistryCommand should return success false (idempotent) when repository doesn't exist" {
        runTest {
            val repoId = RegistryId.random()
            val command = DeleteRegistryCommand(registryId = repoId, requestedBy = testUserId)
            coEvery { registryRepository.findById(repoId) } returns Result.success(null)

            val result = registryService.handle(command)

            result.shouldBeSuccess { it shouldBe false }
            coVerify(exactly = 0) { registryRepository.deleteById(any()) }
        }
    }

    "handle DeleteRegistryCommand should return success false if repository deleteById returns false" {
        runTest {
            val repoId = RegistryId.random()
            val existingRepo = HostedRegistry(
                id = repoId, name = "delete-fails-repo", format = ArtifactType.GENERIC,
                description = "Delete will fail in repo", storageConfig = defaultStorageConfig
            )
            val command = DeleteRegistryCommand(registryId = repoId, requestedBy = testUserId)

            coEvery { registryRepository.findById(repoId) } returns Result.success(existingRepo)
            coEvery { registryRepository.deleteById(repoId) } returns Result.success(false)

            val result = registryService.handle(command)

            result.shouldBeSuccess { it shouldBe false }
            verify(exactly = 0) { eventPublisher(any()) }
        }
    }


    // --- Find Registry Tests ---
    "handle FindRegistriesByFormatCommand should return list of registries" {
        runTest {
            val format = ArtifactType.MAVEN
            val command = FindRegistriesByFormatCommand(format = format, requestedBy = testUserId)
            val expectedRepos = listOf(
                HostedRegistry(
                    id = RegistryId.random(),
                    name = "maven1",
                    format = format,
                    storageConfig = defaultStorageConfig,
                    online = defaultOnlineStatus
                ),
                ProxyRegistry(
                    id = RegistryId.random(),
                    name = "maven-proxy",
                    format = format,
                    storageConfig = defaultStorageConfig,
                    proxyConfig = ProxyConfig("http://r.c"),
                    online = defaultOnlineStatus
                )
            )
            coEvery { registryRepository.findAll(format) } returns Result.success(expectedRepos)

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it shouldBe expectedRepos
            }
        }
    }

    "handle FindRegistriesByFormatCommand should return empty list if no registries match format" {
        runTest {
            val format = ArtifactType.PYPI
            val command = FindRegistriesByFormatCommand(format = format, requestedBy = testUserId)
            coEvery { registryRepository.findAll(format) } returns Result.success(emptyList())

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it.shouldBeEmpty()
            }
        }
    }

    "handle FindRegistryByIdCommand should return registry if found" {
        runTest {
            val repoId = RegistryId.random()
            val command = FindRegistryByIdCommand(registryId = repoId, requestedBy = testUserId)
            val expectedRepo = HostedRegistry(
                id = repoId,
                name = "found-by-id",
                format = ArtifactType.NPM,
                storageConfig = defaultStorageConfig,
                online = defaultOnlineStatus
            )
            coEvery { registryRepository.findById(repoId) } returns Result.success(expectedRepo)

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it shouldBe expectedRepo
            }
        }
    }

    "handle FindRegistryByIdCommand should return null if not found" {
        runTest {
            val repoId = RegistryId.random()
            val command = FindRegistryByIdCommand(registryId = repoId, requestedBy = testUserId)
            coEvery { registryRepository.findById(repoId) } returns Result.success(null)

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it shouldBe null
            }
        }
    }


    "handle FindRegistryByNameCommand should return registry if found" {
        runTest {
            val name = "found-by-name"
            val command = FindRegistryByNameCommand(name = name, requestedBy = testUserId)
            val expectedRepo = GroupRegistry(
                id = RegistryId.random(),
                name = name,
                format = ArtifactType.DOCKER,
                storageConfig = defaultStorageConfig,
                groupConfig = GroupConfig(emptyList()),
                online = defaultOnlineStatus
            )
            coEvery { registryRepository.findByName(name) } returns Result.success(expectedRepo)

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it shouldBe expectedRepo
            }
        }
    }

    "handle FindRegistryByNameCommand should return null if not found" {
        runTest {
            val name = "not-found-by-name"
            val command = FindRegistryByNameCommand(name = name, requestedBy = testUserId)
            coEvery { registryRepository.findByName(name) } returns Result.success(null)

            val result = registryService.handle(command)

            result.shouldBeSuccess {
                it shouldBe null
            }
        }
    }

    "handle FindRegistryByIdCommand should return failure if repository call fails" {
        runTest {
            val repoId = RegistryId.random()
            val command = FindRegistryByIdCommand(registryId = repoId, requestedBy = testUserId)
            val dbError = RuntimeException("DB error during findById")
            // This mock simulates the repository itself returning a failure Result
            coEvery { registryRepository.findById(repoId) } returns Result.failure(dbError)

            val result = registryService.handle(command)

            result.shouldBeFailure {
                // In the find operations, if the repository returns a Result.failure,
                // the service's try-catch block for general exceptions is NOT hit.
                // The Result.failure from the repository is returned directly.
                // So, 'it' is the original dbError.
                it shouldBe dbError
            }
        }
    }
})