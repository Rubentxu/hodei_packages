package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.registry.ArtifactRegistryEvent
import dev.rubentxu.hodei.packages.domain.model.registry.Registry
import dev.rubentxu.hodei.packages.domain.model.registry.RegistryType
import dev.rubentxu.hodei.packages.domain.model.registry.StorageType
import dev.rubentxu.hodei.packages.domain.repository.RegistryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest

class RepositoryServiceTest : StringSpec({
    
    // Configuración común
    lateinit var registryRepository: RegistryRepository
    lateinit var eventPublisher: (ArtifactRegistryEvent) -> Unit
    lateinit var repositoryService: RepositoryService
    
    beforeTest {
        registryRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        repositoryService = RepositoryService(registryRepository, eventPublisher)
    }
    
    "createRepository should create and persist a new repository" { runTest {
        // Arrange
        val name = "test-repo"
        val type = RegistryType.MAVEN
        val description = "Test repository"
        val isPublic = true
        val createdBy = UUID.randomUUID()
        
        coEvery { registryRepository.existsByName(name) } returns false
        coEvery { registryRepository.save(any()) } answers { firstArg() }
        
        // Act
        val result = repositoryService.createRepository(
            name = name,
            type = type,
            description = description,
            isPublic = isPublic,
            createdBy = createdBy
        )
        
        // Assert
        result.name shouldBe name
        result.type shouldBe type
        result.description shouldBe description
        result.isPublic shouldBe isPublic
        result.createdBy shouldBe createdBy
        
        coVerify { registryRepository.existsByName(name) }
        coVerify { registryRepository.save(any()) }
        verify { eventPublisher(match { it is ArtifactRegistryEvent.ArtifactRegistryCreated }) }
    } }
    
    "createRepository should throw exception when repository name already exists" { runTest {
        // Arrange
        val name = "existing-repo"
        coEvery { registryRepository.existsByName(name) } returns true
        
        // Act & Assert
        shouldThrow<IllegalStateException> {
            repositoryService.createRepository(
                name = name,
                type = RegistryType.NPM,
                description = "This should fail",
                isPublic = true,
                createdBy = UUID.randomUUID()
            )
        }.message shouldBe "A repository with name '$name' already exists"
        
        coVerify { registryRepository.existsByName(name) }
        coVerify(exactly = 0) { registryRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "updateRepository should update and persist changes" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val updatedBy = UUID.randomUUID()
        val existingRepo = Registry(
            id = repoId,
            name = "test-repo",
            type = RegistryType.MAVEN,
            description = "Original description",
            isPublic = false,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            storageType = StorageType.LOCAL
        )
        
        val newDescription = "Updated description"
        val newIsPublic = true
        
        coEvery { registryRepository.findById(repoId) } returns existingRepo
        coEvery { registryRepository.save(any()) } answers { firstArg() }
        
        // Act
        val result = repositoryService.updateRepository(
            id = repoId,
            description = newDescription,
            isPublic = newIsPublic,
            updatedBy = updatedBy
        )
        
        // Assert
        result.id shouldBe repoId
        result.description shouldBe newDescription
        result.isPublic shouldBe newIsPublic
        
        coVerify { registryRepository.findById(repoId) }
        coVerify { registryRepository.save(any()) }
        verify { eventPublisher(match { it is ArtifactRegistryEvent.ArtifactRegistryUpdated }) }
    } }
    
    "updateRepository should throw exception when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        coEvery { registryRepository.findById(repoId) } returns null
        
        // Act & Assert
        shouldThrow<IllegalArgumentException> {
            repositoryService.updateRepository(
                id = repoId,
                description = "New description",
                updatedBy = UUID.randomUUID()
            )
        }.message shouldBe "Repository with ID '$repoId' not found"
        
        coVerify { registryRepository.findById(repoId) }
        coVerify(exactly = 0) { registryRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "deleteRepository should delete and emit event when repository exists" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val existingRepo = Registry(
            id = repoId,
            name = "repo-to-delete",
            type = RegistryType.NPM,
            description = "Will be deleted",
            isPublic = true,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            storageType = StorageType.LOCAL
        )
        
        coEvery { registryRepository.findById(repoId) } returns existingRepo
        coEvery { registryRepository.deleteById(repoId) } returns true
        
        // Act
        val result = repositoryService.deleteRepository(repoId, deletedBy)
        
        // Assert
        result shouldBe true
        
        coVerify { registryRepository.findById(repoId) }
        coVerify { registryRepository.deleteById(repoId) }
        verify { eventPublisher(match { it is ArtifactRegistryEvent.ArtifactRegistryDeleted }) }
    } }
    
    "deleteRepository should return false when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        coEvery { registryRepository.findById(repoId) } returns null
        
        // Act
        val result = repositoryService.deleteRepository(repoId, UUID.randomUUID())
        
        // Assert
        result shouldBe false
        
        coVerify { registryRepository.findById(repoId) }
        coVerify(exactly = 0) { registryRepository.deleteById(any()) }
        coVerify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "changeRepositoryVisibility should update visibility and emit event" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val updatedBy = UUID.randomUUID()
        val existingRepo = Registry(
            id = repoId,
            name = "test-repo",
            type = RegistryType.MAVEN,
            description = "Test repository",
            isPublic = false,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600),
            storageType = StorageType.LOCAL
        )
        
        val newVisibility = true
        
        coEvery { registryRepository.findById(repoId) } returns existingRepo
        coEvery { registryRepository.save(any()) } answers { firstArg() }
        
        // Act
        val result = repositoryService.changeRepositoryVisibility(
            id = repoId,
            isPublic = newVisibility,
            updatedBy = updatedBy
        )
        
        // Assert
        result.isPublic shouldBe newVisibility
        
        coVerify { registryRepository.findById(repoId) }
        coVerify { registryRepository.save(any()) }
        verify { eventPublisher(match { it is ArtifactRegistryEvent.ArtifactRegistryAccessChanged }) }
    } }
    
    "findRepositories should return filtered repositories" { runTest {
        // Arrange
        val type = RegistryType.MAVEN
        val repos = listOf(
            Registry(
                id = UUID.randomUUID(),
                name = "maven-repo-1",
                type = RegistryType.MAVEN,
                description = "Maven repository 1",
                isPublic = true,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                storageType = StorageType.LOCAL
            ),
            Registry(
                id = UUID.randomUUID(),
                name = "maven-repo-2",
                type = RegistryType.MAVEN,
                description = "Maven repository 2",
                isPublic = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                storageType = StorageType.LOCAL
            )
        )
        
        coEvery { registryRepository.findAll(type) } returns repos
        
        // Act
        val result = repositoryService.findRepositories(type)
        
        // Assert
        result shouldBe repos
        
        coVerify { registryRepository.findAll(type) }
    } }
    
    "findRepositoryById should return repository when exists" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val existingRepo = Registry(
            id = repoId,
            name = "test-repo",
            type = RegistryType.NPM,
            description = "Test repository",
            isPublic = true,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            storageType = StorageType.LOCAL
        )
        
        coEvery { registryRepository.findById(repoId) } returns existingRepo
        
        // Act
        val result = repositoryService.findRepositoryById(repoId)
        
        // Assert
        result shouldBe existingRepo
        
        coVerify { registryRepository.findById(repoId) }
    } }
    
    "findRepositoryById should return null when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        
        coEvery { registryRepository.findById(repoId) } returns null
        
        // Act
        val result = repositoryService.findRepositoryById(repoId)
        
        // Assert
        result shouldBe null
        
        coVerify { registryRepository.findById(repoId) }
    } }
})