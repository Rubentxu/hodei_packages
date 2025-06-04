package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.registry.ArtifactRegistryEvent
import dev.rubentxu.hodei.packages.domain.model.registry.Registry
import dev.rubentxu.hodei.packages.domain.model.registry.RegistryType
import dev.rubentxu.hodei.packages.domain.model.registry.StorageType
import dev.rubentxu.hodei.packages.domain.repository.RegistryRepository
import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con la gestión de registros de artefactos.
 * Este servicio utiliza el puerto ArtifactRegistryRepository para persistencia y emite eventos de dominio.
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
        createdBy: UUID
    ): Registry {
        // Verificar si ya existe un registro de artefactos con el mismo nombre
        if (registryRepository.existsByName(name)) {
            throw IllegalStateException("An artifact registry with name '$name' already exists")
        }
        
        val now = Instant.now()
        val registry = Registry(
            id = UUID.randomUUID(),
            name = name,
            type = type,
            description = description,
            createdBy = createdBy,
            createdAt = now,
            updatedAt = now,
            isPublic = isPublic,
            storageType =  StorageType.LOCAL
        )
        
        // Persistir el registro de artefactos
        val savedArtifactRegistry = registryRepository.save(registry)
        
        // Publicar evento de creación de registro de artefactos
        eventPublisher(
            ArtifactRegistryEvent.ArtifactRegistryCreated(
                name = savedArtifactRegistry.name,
                type = savedArtifactRegistry.type,
                createdBy = savedArtifactRegistry.createdBy,
                timestamp = savedArtifactRegistry.createdAt,
                registryId = savedArtifactRegistry.id
            )
        )
        
        return savedArtifactRegistry
    }
    
    /**
     * Actualiza un registro de artefactos existente.
     * @param id ID del registro de artefactos a actualizar
     * @param description Nueva descripción (opcional)
     * @param isPublic Nuevo estado de visibilidad (opcional)
     * @param updatedBy ID del usuario que realiza la actualización
     * @return El registro de artefactos actualizado
     * @throws IllegalArgumentException si el registro de artefactos no existe
     */
    suspend fun updateArtifactRegistry(
        id: UUID,
        description: String? = null,
        isPublic: Boolean? = null,
        updatedBy: UUID
    ): Registry {
        val artifactRegistry = registryRepository.findById(id)
            ?: throw IllegalArgumentException("ArtifactRegistry with ID '$id' not found")
        
        val changes = mutableMapOf<String, Any?>()
        
        // Crear una nueva instancia con los cambios aplicados
        val updatedArtifactRegistry = artifactRegistry.copy(
            description = description?.also { changes["description"] = it } ?: artifactRegistry.description,
            isPublic = isPublic?.also { changes["isPublic"] = it } ?: artifactRegistry.isPublic,
            updatedAt = Instant.now()
        )
        
        // Solo persistir si hay cambios
        if (changes.isNotEmpty()) {
            val savedArtifactRegistry = registryRepository.save(updatedArtifactRegistry)
            
            // Publicar evento de actualización
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
    
    /**
     * Elimina un registro de artefactos del sistema.
     * @param id ID del registro de artefactos a eliminar
     * @param deletedBy ID del usuario que realiza la eliminación
     * @return true si se eliminó correctamente, false si no existía
     */
    suspend fun deleteArtifactRegistry(id: UUID, deletedBy: UUID): Boolean {
        val artifactRegistry = registryRepository.findById(id)
            ?: return false
        
        val deleted = registryRepository.deleteById(id)
        
        if (deleted) {
            // Publicar evento de eliminación
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
    
    /**
     * Cambia la visibilidad de un registro de artefactos.
     * @param id ID del registro de artefactos
     * @param isPublic Nuevo estado de visibilidad
     * @param updatedBy ID del usuario que realiza el cambio
     * @return El registro de artefactos actualizado
     * @throws IllegalArgumentException si el registro de artefactos no existe
     */
    suspend fun changeArtifactRegistryVisibility(id: UUID, isPublic: Boolean, updatedBy: UUID): Registry {
        val artifactRegistry = registryRepository.findById(id)
            ?: throw IllegalArgumentException("ArtifactRegistry with ID '$id' not found")
        
        // Solo actualizar si la visibilidad cambió
        if (artifactRegistry.isPublic != isPublic) {
            val updatedArtifactRegistry = artifactRegistry.copy(
                isPublic = isPublic,
                updatedAt = Instant.now()
            )
            
            val savedArtifactRegistry = registryRepository.save(updatedArtifactRegistry)
            
            // Publicar evento específico de cambio de acceso
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
    
    /**
     * Busca registros de artefactos según criterios.
     * @param type Tipo de registro de artefactos (opcional)
     * @return Lista de registros de artefactos que coinciden con los criterios
     */
    suspend fun findArtifactRegistries(type: RegistryType? = null): List<Registry> {
        return registryRepository.findAll(type)
    }
    
    /**
     * Busca un registro de artefactos por su ID.
     * @param id ID del registro de artefactos
     * @return El registro de artefactos si existe, null en caso contrario
     */
    suspend fun findArtifactRegistryById(id: UUID): Registry? {
        return registryRepository.findById(id)
    }
    
    /**
     * Busca un registro de artefactos por su nombre.
     * @param name Nombre del registro de artefactos
     * @return El registro de artefactos si existe, null en caso contrario
     */
    suspend fun findArtifactRegistryByName(name: String): Registry? {
        return registryRepository.findByName(name)
    }
}
