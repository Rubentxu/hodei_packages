package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.repository.RepositoryEvent
import dev.rubentxu.hodei.packages.domain.model.repository.Repository
import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import dev.rubentxu.hodei.packages.domain.model.repository.StorageType
import dev.rubentxu.hodei.packages.domain.repository.RepositoryRepository
import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con la gestión de repositorios.
 * Este servicio utiliza el puerto RepositoryRepository para persistencia y emite eventos de dominio.
 */
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val eventPublisher: (RepositoryEvent) -> Unit
) {
    /**
     * Crea un nuevo repositorio en el sistema.
     * @param name Nombre del repositorio
     * @param type Tipo del repositorio (MAVEN, NPM)
     * @param description Descripción del repositorio
     * @param isPublic Si el repositorio es público o privado
     * @param createdBy ID del usuario que crea el repositorio
     * @return El repositorio creado
     * @throws IllegalStateException si ya existe un repositorio con el mismo nombre
     */
    suspend fun createRepository(
        name: String,
        type: RepositoryType,
        description: String,
        isPublic: Boolean,
        createdBy: UUID
    ): Repository {
        // Verificar si ya existe un repositorio con el mismo nombre
        if (repositoryRepository.existsByName(name)) {
            throw IllegalStateException("A repository with name '$name' already exists")
        }
        
        val now = Instant.now()
        val repository = Repository(
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
        
        // Persistir el repositorio
        val savedRepository = repositoryRepository.save(repository)
        
        // Publicar evento de creación de repositorio
        eventPublisher(
            RepositoryEvent.RepositoryCreated(
                repositoryId = savedRepository.id,
                name = savedRepository.name,
                type = savedRepository.type,
                createdBy = savedRepository.createdBy,
                timestamp = savedRepository.createdAt
            )
        )
        
        return savedRepository
    }
    
    /**
     * Actualiza un repositorio existente.
     * @param id ID del repositorio a actualizar
     * @param description Nueva descripción (opcional)
     * @param isPublic Nuevo estado de visibilidad (opcional)
     * @param updatedBy ID del usuario que realiza la actualización
     * @return El repositorio actualizado
     * @throws IllegalArgumentException si el repositorio no existe
     */
    suspend fun updateRepository(
        id: UUID,
        description: String? = null,
        isPublic: Boolean? = null,
        updatedBy: UUID
    ): Repository {
        val repository = repositoryRepository.findById(id)
            ?: throw IllegalArgumentException("Repository with ID '$id' not found")
        
        val changes = mutableMapOf<String, Any?>()
        
        // Crear una nueva instancia con los cambios aplicados
        val updatedRepository = repository.copy(
            description = description?.also { changes["description"] = it } ?: repository.description,
            isPublic = isPublic?.also { changes["isPublic"] = it } ?: repository.isPublic,
            updatedAt = Instant.now()
        )
        
        // Solo persistir si hay cambios
        if (changes.isNotEmpty()) {
            val savedRepository = repositoryRepository.save(updatedRepository)
            
            // Publicar evento de actualización
            eventPublisher(
                RepositoryEvent.RepositoryUpdated(
                    repositoryId = savedRepository.id,
                    name = savedRepository.name,
                    updatedBy = updatedBy,
                    timestamp = savedRepository.updatedAt,
                    changes = changes
                )
            )
            
            return savedRepository
        }
        
        return repository
    }
    
    /**
     * Elimina un repositorio del sistema.
     * @param id ID del repositorio a eliminar
     * @param deletedBy ID del usuario que realiza la eliminación
     * @return true si se eliminó correctamente, false si no existía
     */
    suspend fun deleteRepository(id: UUID, deletedBy: UUID): Boolean {
        val repository = repositoryRepository.findById(id)
            ?: return false
        
        val deleted = repositoryRepository.deleteById(id)
        
        if (deleted) {
            // Publicar evento de eliminación
            eventPublisher(
                RepositoryEvent.RepositoryDeleted(
                    repositoryId = repository.id,
                    name = repository.name,
                    deletedBy = deletedBy,
                    timestamp = Instant.now()
                )
            )
        }
        
        return deleted
    }
    
    /**
     * Cambia la visibilidad de un repositorio.
     * @param id ID del repositorio
     * @param isPublic Nuevo estado de visibilidad
     * @param updatedBy ID del usuario que realiza el cambio
     * @return El repositorio actualizado
     * @throws IllegalArgumentException si el repositorio no existe
     */
    suspend fun changeRepositoryVisibility(id: UUID, isPublic: Boolean, updatedBy: UUID): Repository {
        val repository = repositoryRepository.findById(id)
            ?: throw IllegalArgumentException("Repository with ID '$id' not found")
        
        // Solo actualizar si la visibilidad cambió
        if (repository.isPublic != isPublic) {
            val updatedRepository = repository.copy(
                isPublic = isPublic,
                updatedAt = Instant.now()
            )
            
            val savedRepository = repositoryRepository.save(updatedRepository)
            
            // Publicar evento específico de cambio de acceso
            eventPublisher(
                RepositoryEvent.RepositoryAccessChanged(
                    repositoryId = savedRepository.id,
                    name = savedRepository.name,
                    isPublic = savedRepository.isPublic,
                    updatedBy = updatedBy,
                    timestamp = savedRepository.updatedAt
                )
            )
            
            return savedRepository
        }
        
        return repository
    }
    
    /**
     * Busca repositorios según criterios.
     * @param type Tipo de repositorio (opcional)
     * @return Lista de repositorios que coinciden con los criterios
     */
    suspend fun findRepositories(type: RepositoryType? = null): List<Repository> {
        return repositoryRepository.findAll(type)
    }
    
    /**
     * Busca un repositorio por su ID.
     * @param id ID del repositorio
     * @return El repositorio si existe, null en caso contrario
     */
    suspend fun findRepositoryById(id: UUID): Repository? {
        return repositoryRepository.findById(id)
    }
    
    /**
     * Busca un repositorio por su nombre.
     * @param name Nombre del repositorio
     * @return El repositorio si existe, null en caso contrario
     */
    suspend fun findRepositoryByName(name: String): Repository? {
        return repositoryRepository.findByName(name)
    }
}