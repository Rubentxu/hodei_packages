package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.repository.RepositoryEvent
import dev.rubentxu.hodei.packages.domain.model.repository.Repository
import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import dev.rubentxu.hodei.packages.domain.repository.RepositoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest

class RepositoryServiceTest : StringSpec({
    
    // Configuración común
    lateinit var repositoryRepository: RepositoryRepository
    lateinit var eventPublisher: (RepositoryEvent) -> Unit
    lateinit var repositoryService: RepositoryService
    
    beforeTest {
        repositoryRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        repositoryService = RepositoryService(repositoryRepository, eventPublisher)
    }
    
    "createRepository should create and persist a new repository" { runTest {
        // Arrange
        val name = "test-repo"
        val type = RepositoryType.MAVEN
        val description = "Test repository"
        val isPublic = true
        val createdBy = UUID.randomUUID()
        
        coEvery { repositoryRepository.existsByName(name) } returns false
        coEvery { repositoryRepository.save(any()) } answers { firstArg() }
        
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
        
        coVerify { repositoryRepository.existsByName(name) }
        coVerify { repositoryRepository.save(any()) }
        verify { eventPublisher(match { it is RepositoryEvent.RepositoryCreated }) }
    } }
    
    "createRepository should throw exception when repository name already exists" { runTest {
        // Arrange
        val name = "existing-repo"
        coEvery { repositoryRepository.existsByName(name) } returns true
        
        // Act & Assert
        shouldThrow<IllegalStateException> {
            repositoryService.createRepository(
                name = name,
                type = RepositoryType.NPM,
                description = "This should fail",
                isPublic = true,
                createdBy = UUID.randomUUID()
            )
        }.message shouldBe "A repository with name '$name' already exists"
        
        coVerify { repositoryRepository.existsByName(name) }
        coVerify(exactly = 0) { repositoryRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "updateRepository should update and persist changes" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val updatedBy = UUID.randomUUID()
        val existingRepo = Repository(
            id = repoId,
            name = "test-repo",
            type = RepositoryType.MAVEN,
            description = "Original description",
            isPublic = false,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        
        val newDescription = "Updated description"
        val newIsPublic = true
        
        coEvery { repositoryRepository.findById(repoId) } returns existingRepo
        coEvery { repositoryRepository.save(any()) } answers { firstArg() }
        
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
        
        coVerify { repositoryRepository.findById(repoId) }
        coVerify { repositoryRepository.save(any()) }
        verify { eventPublisher(match { it is RepositoryEvent.RepositoryUpdated }) }
    } }
    
    "updateRepository should throw exception when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        coEvery { repositoryRepository.findById(repoId) } returns null
        
        // Act & Assert
        shouldThrow<IllegalArgumentException> {
            repositoryService.updateRepository(
                id = repoId,
                description = "New description",
                updatedBy = UUID.randomUUID()
            )
        }.message shouldBe "Repository with ID '$repoId' not found"
        
        coVerify { repositoryRepository.findById(repoId) }
        coVerify(exactly = 0) { repositoryRepository.save(any()) }
        verify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "deleteRepository should delete and emit event when repository exists" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val existingRepo = Repository(
            id = repoId,
            name = "repo-to-delete",
            type = RepositoryType.NPM,
            description = "Will be deleted",
            isPublic = true,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        
        coEvery { repositoryRepository.findById(repoId) } returns existingRepo
        coEvery { repositoryRepository.deleteById(repoId) } returns true
        
        // Act
        val result = repositoryService.deleteRepository(repoId, deletedBy)
        
        // Assert
        result shouldBe true
        
        coVerify { repositoryRepository.findById(repoId) }
        coVerify { repositoryRepository.deleteById(repoId) }
        verify { eventPublisher(match { it is RepositoryEvent.RepositoryDeleted }) }
    } }
    
    "deleteRepository should return false when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        coEvery { repositoryRepository.findById(repoId) } returns null
        
        // Act
        val result = repositoryService.deleteRepository(repoId, UUID.randomUUID())
        
        // Assert
        result shouldBe false
        
        coVerify { repositoryRepository.findById(repoId) }
        coVerify(exactly = 0) { repositoryRepository.deleteById(any()) }
        coVerify(exactly = 0) { eventPublisher(any()) }
    } }
    
    "changeRepositoryVisibility should update visibility and emit event" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val updatedBy = UUID.randomUUID()
        val existingRepo = Repository(
            id = repoId,
            name = "test-repo",
            type = RepositoryType.MAVEN,
            description = "Test repository",
            isPublic = false,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        
        val newVisibility = true
        
        coEvery { repositoryRepository.findById(repoId) } returns existingRepo
        coEvery { repositoryRepository.save(any()) } answers { firstArg() }
        
        // Act
        val result = repositoryService.changeRepositoryVisibility(
            id = repoId,
            isPublic = newVisibility,
            updatedBy = updatedBy
        )
        
        // Assert
        result.isPublic shouldBe newVisibility
        
        coVerify { repositoryRepository.findById(repoId) }
        coVerify { repositoryRepository.save(any()) }
        verify { eventPublisher(match { it is RepositoryEvent.RepositoryAccessChanged }) }
    } }
    
    "findRepositories should return filtered repositories" { runTest {
        // Arrange
        val type = RepositoryType.MAVEN
        val repos = listOf(
            Repository(
                id = UUID.randomUUID(),
                name = "maven-repo-1",
                type = RepositoryType.MAVEN,
                description = "Maven repository 1",
                isPublic = true,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Repository(
                id = UUID.randomUUID(),
                name = "maven-repo-2",
                type = RepositoryType.MAVEN,
                description = "Maven repository 2",
                isPublic = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        
        coEvery { repositoryRepository.findAll(type) } returns repos
        
        // Act
        val result = repositoryService.findRepositories(type)
        
        // Assert
        result shouldBe repos
        
        coVerify { repositoryRepository.findAll(type) }
    } }
    
    "findRepositoryById should return repository when exists" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        val existingRepo = Repository(
            id = repoId,
            name = "test-repo",
            type = RepositoryType.NPM,
            description = "Test repository",
            isPublic = true,
            createdBy = UUID.randomUUID(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { repositoryRepository.findById(repoId) } returns existingRepo
        
        // Act
        val result = repositoryService.findRepositoryById(repoId)
        
        // Assert
        result shouldBe existingRepo
        
        coVerify { repositoryRepository.findById(repoId) }
    } }
    
    "findRepositoryById should return null when repository doesn't exist" { runTest {
        // Arrange
        val repoId = UUID.randomUUID()
        
        coEvery { repositoryRepository.findById(repoId) } returns null
        
        // Act
        val result = repositoryService.findRepositoryById(repoId)
        
        // Assert
        result shouldBe null
        
        coVerify { repositoryRepository.findById(repoId) }
    } }
})