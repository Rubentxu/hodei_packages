package dev.rubentxu.hodei.packages.domain.registrymanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.events.ArtifactRegistryEvent
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.Registry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.StorageType
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con la gestión de registros de artefactos.
 * Este servicio utiliza el puerto RegistryRepository para persistencia y emite eventos de dominio.
 */
class RegistryService(
    private val registryRepository: RegistryRepository,
    private val eventPublisher: (ArtifactRegistryEvent) -> Unit
) {
    /**
     * Crea un nuevo registro de artefactos en el sistema.
     * @param name Nombre del registro de artefactos
     * @param type Tipo del registro de artefactos (MAVEN, NPM)
     * @param description Descripción del registro de artefactos
     * @param isPublic Si el registro de artefactos es público o privado
     * @param createdBy ID del usuario que crea el registro de artefactos
     * @return El registro de artefactos creado
     * @throws IllegalStateException si ya existe un registro de artefactos con el mismo nombre
     */
    suspend fun createArtifactRegistry(
        name: String,
        type: RegistryType,
        description: String,
        isPublic: Boolean,
        createdBy: UserId
    ): Registry {
        val existsResult = registryRepository.existsByName(name)
        if (existsResult.isFailure) {
            // Manejar el error de la llamada al repositorio, quizás relanzar o devolver un error
            throw existsResult.exceptionOrNull() ?: IllegalStateException("Error checking if registry exists by name")
        }
        if (existsResult.getOrNull() == true) {
            throw IllegalStateException("An artifact registry with name '$name' already exists")
        }
        
        val now = Instant.now()
        val registry = Registry(
            id = UUID.randomUUID(),
            name = name,
            type = type,
            description = description,
            createdBy = createdBy, // Considerar UserId de artifactmanagement si es el mismo concepto
            createdAt = now,
            updatedAt = now,
            isPublic = isPublic,
            storageType =  StorageType.LOCAL
        )
        
        val savedResult = registryRepository.save(registry)
        if (savedResult.isFailure) {
             throw savedResult.exceptionOrNull() ?: IllegalStateException("Error saving registry")
        }
        val savedArtifactRegistry = savedResult.getOrNull()!!
        
        eventPublisher(
            ArtifactRegistryEvent.ArtifactRegistryCreated(
                registryId = savedArtifactRegistry.id,
                name = savedArtifactRegistry.name,
                type = savedArtifactRegistry.type,
                createdBy = savedArtifactRegistry.createdBy,
                timestamp = savedArtifactRegistry.createdAt
            )
        )
        
        return savedArtifactRegistry
    }
    
    suspend fun updateArtifactRegistry(
        id: UUID,
        description: String? = null,
        isPublic: Boolean? = null,
        updatedBy: UUID
    ): Registry {
        val findResult = registryRepository.findById(id)
        if (findResult.isFailure) {
            throw findResult.exceptionOrNull() ?: IllegalStateException("Error finding registry by id")
        }
        val artifactRegistry = findResult.getOrNull()
            ?: throw IllegalArgumentException("ArtifactRegistry with ID '$id' not found")
        
        val changes = mutableMapOf<String, Any?>()
        
        val updatedArtifactRegistry = artifactRegistry.copy(
            description = description?.also { changes["description"] = it } ?: artifactRegistry.description,
            isPublic = isPublic?.also { changes["isPublic"] = it } ?: artifactRegistry.isPublic,
            updatedAt = Instant.now()
        )
        
        if (changes.isNotEmpty()) {
            val savedResult = registryRepository.save(updatedArtifactRegistry)
            if (savedResult.isFailure) {
                throw savedResult.exceptionOrNull() ?: IllegalStateException("Error saving updated registry")
            }
            val savedArtifactRegistry = savedResult.getOrNull()!!
            
            eventPublisher(
                ArtifactRegistryEvent.ArtifactRegistryUpdated(
                    registryId = savedArtifactRegistry.id,
                    name = savedArtifactRegistry.name,
                    updatedBy = updatedBy,
                    timestamp = savedArtifactRegistry.updatedAt,
                    changes = changes
                )
            )
            
            return savedArtifactRegistry
        }
        
        return artifactRegistry
    }
    
    suspend fun deleteArtifactRegistry(id: UUID, deletedBy: UUID): Boolean {
        val findResult = registryRepository.findById(id)
        if (findResult.isFailure) {
            throw findResult.exceptionOrNull() ?: IllegalStateException("Error finding registry by id before deletion")
        }
        val artifactRegistry = findResult.getOrNull() ?: return false
        
        val deletedResult = registryRepository.deleteById(id)
        if (deletedResult.isFailure) {
            throw deletedResult.exceptionOrNull() ?: IllegalStateException("Error deleting registry by id")
        }
        val deleted = deletedResult.getOrNull()!!
        
        if (deleted) {
            eventPublisher(
                ArtifactRegistryEvent.ArtifactRegistryDeleted(
                    registryId = artifactRegistry.id,
                    name = artifactRegistry.name,
                    deletedBy = deletedBy,
                    timestamp = Instant.now()
                )
            )
        }
        
        return deleted
    }
    
    suspend fun changeArtifactRegistryVisibility(id: UUID, isPublic: Boolean, updatedBy: UUID): Registry {
        val findResult = registryRepository.findById(id)
        if (findResult.isFailure) {
            throw findResult.exceptionOrNull() ?: IllegalStateException("Error finding registry by id for visibility change")
        }
        val artifactRegistry = findResult.getOrNull()
            ?: throw IllegalArgumentException("ArtifactRegistry with ID '$id' not found")
        
        if (artifactRegistry.isPublic != isPublic) {
            val updatedArtifactRegistry = artifactRegistry.copy(
                isPublic = isPublic,
                updatedAt = Instant.now()
            )
            
            val savedResult = registryRepository.save(updatedArtifactRegistry)
            if (savedResult.isFailure) {
                throw savedResult.exceptionOrNull() ?: IllegalStateException("Error saving registry after visibility change")
            }
            val savedArtifactRegistry = savedResult.getOrNull()!!
            
            eventPublisher(
                ArtifactRegistryEvent.ArtifactRegistryAccessChanged(
                    registryId = savedArtifactRegistry.id,
                    name = savedArtifactRegistry.name,
                    isPublic = savedArtifactRegistry.isPublic,
                    updatedBy = updatedBy,
                    timestamp = savedArtifactRegistry.updatedAt
                )
            )
            
            return savedArtifactRegistry
        }
        
        return artifactRegistry
    }
    
    suspend fun findArtifactRegistries(type: RegistryType? = null): List<Registry> {
        val result = registryRepository.findAll(type)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: IllegalStateException("Error finding all registries")
        }
        return result.getOrNull() ?: emptyList()
    }
    
    suspend fun findArtifactRegistryById(id: UUID): Registry? {
        val result = registryRepository.findById(id)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: IllegalStateException("Error finding registry by id")
        }
        return result.getOrNull()
    }
    
    suspend fun findArtifactRegistryByName(name: String): Registry? {
        val result = registryRepository.findByName(name)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: IllegalStateException("Error finding registry by name")
        }
        return result.getOrNull()
    }
} 