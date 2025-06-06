package dev.rubentxu.hodei.packages.domain.registrymanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryType
import java.time.Instant
import java.util.UUID

/**
 * Eventos de dominio relacionados con operaciones de repositorios.
 * Estos eventos son emitidos cuando ocurren cambios significativos en los repositorios.
 */
sealed class ArtifactRegistryEvent {
    /**
     * Evento emitido cuando se crea un nuevo repositorio.
     */
    data class ArtifactRegistryCreated(
        val registryId: UUID,
        val name: String,
        val type: RegistryType,
        val createdBy: UserId,
        val timestamp: Instant,
    ) : ArtifactRegistryEvent()

    /**
     * Evento emitido cuando se actualiza un repositorio existente.
     */
    data class ArtifactRegistryUpdated(
        val registryId: UUID,
        val name: String,
        val updatedBy: UUID,
        val timestamp: Instant,
        val changes: Map<String, Any?>,
    ) : ArtifactRegistryEvent()

    /**
     * Evento emitido cuando se elimina un repositorio.
     */
    data class ArtifactRegistryDeleted(
        val registryId: UUID,
        val name: String,
        val deletedBy: UUID,
        val timestamp: Instant,
    ) : ArtifactRegistryEvent()

    /**
     * Evento emitido cuando cambia la configuración de acceso de un repositorio.
     */
    data class ArtifactRegistryAccessChanged(
        val registryId: UUID,
        val name: String,
        val isPublic: Boolean,
        val updatedBy: UUID,
        val timestamp: Instant,
    ) : ArtifactRegistryEvent()
} 