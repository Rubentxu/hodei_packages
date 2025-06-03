package dev.rubentxu.hodei.packages.domain.events.repository

import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import java.time.Instant
import java.util.UUID

/**
 * Eventos de dominio relacionados con operaciones de repositorios.
 * Estos eventos son emitidos cuando ocurren cambios significativos en los repositorios.
 */
sealed class RepositoryEvent {
    /**
     * Evento emitido cuando se crea un nuevo repositorio.
     */
    data class RepositoryCreated(
        val repositoryId: UUID,
        val name: String,
        val type: RepositoryType,
        val createdBy: UUID,
        val timestamp: Instant,
    ) : RepositoryEvent()

    /**
     * Evento emitido cuando se actualiza un repositorio existente.
     */
    data class RepositoryUpdated(
        val repositoryId: UUID,
        val name: String,
        val updatedBy: UUID,
        val timestamp: Instant,
        val changes: Map<String, Any?>,
    ) : RepositoryEvent()

    /**
     * Evento emitido cuando se elimina un repositorio.
     */
    data class RepositoryDeleted(
        val repositoryId: UUID,
        val name: String,
        val deletedBy: UUID,
        val timestamp: Instant,
    ) : RepositoryEvent()

    /**
     * Evento emitido cuando cambia la configuración de acceso de un repositorio.
     */
    data class RepositoryAccessChanged(
        val repositoryId: UUID,
        val name: String,
        val isPublic: Boolean,
        val updatedBy: UUID,
        val timestamp: Instant,
    ) : RepositoryEvent()
}