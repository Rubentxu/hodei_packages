package dev.rubentxu.hodei.packages.domain.events.artifact

import java.time.Instant
import java.util.UUID

/**
 * Eventos de dominio relacionados con operaciones de artefactos.
 * Estos eventos son emitidos cuando ocurren cambios significativos en los artefactos.
 */
sealed class ArtifactEvent {
    /**
     * Evento emitido cuando se sube un nuevo artefacto a un repositorio.
     */
    data class ArtifactUploaded(
        val artifactId: UUID,
        val repositoryId: UUID,
        val groupId: String,
        val artifactName: String,
        val version: String,
        val fileSize: Long,
        val uploadedBy: UUID,
        val timestamp: Instant,
    ) : ArtifactEvent()

    /**
     * Evento emitido cuando se descarga un artefacto.
     */
    data class ArtifactDownloaded(
        val artifactId: UUID,
        val repositoryId: UUID,
        val groupId: String,
        val artifactName: String,
        val version: String,
        val downloadedBy: UUID?,  // Puede ser null si es una descarga anónima
        val timestamp: Instant,
        val clientIp: String?,
        val userAgent: String?
    ) : ArtifactEvent()

    /**
     * Evento emitido cuando se elimina un artefacto.
     */
    data class ArtifactDeleted(
        val artifactId: UUID,
        val repositoryId: UUID,
        val groupId: String,
        val artifactName: String,
        val version: String,
        val deletedBy: UUID,
        val timestamp: Instant,
    ) : ArtifactEvent()

    /**
     * Evento emitido cuando se actualiza un metadato de un artefacto.
     */
    data class ArtifactMetadataUpdated(
        val artifactId: UUID,
        val repositoryId: UUID,
        val groupId: String,
        val artifactName: String,
        val version: String,
        val updatedBy: UUID,
        val timestamp: Instant,
        val updatedMetadata: Map<String, String>
    ) : ArtifactEvent()
}