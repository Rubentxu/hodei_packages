package dev.rubentxu.hodei.packages.domain.artifactmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.MerkleRootHash
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository


import java.time.Instant
import java.util.UUID

/**
 * Domain service that encapsulates business logic related to artifact management.
 * This service coordinates operations between artifacts and repositories, and emits domain events.
 */
class ArtifactService(
    private val artifactRepository: ArtifactRepository,
    private val registryRepository: RegistryRepository,
    private val publishArtifactEvent: (ArtifactPublishedEvent) -> Unit,
    private val publishDownloadEvent: (ArtifactDownloadedEvent) -> Unit,
    private val publishMetadataUpdateEvent: (ArtifactMetadataUpdatedEvent) -> Unit,
    private val publishDeleteEvent: (ArtifactDeletedEvent) -> Unit
) {
    /**
     * Publishes a new artifact in the system.
     * @param registryId UUID of the registry where the artifact will be published.
     * @param group Group of the artifact.
     * @param name Name of the artifact.
     * @param version Version of the artifact.
     * @param createdBy ID of the user publishing the artifact.
     * @param merkleRoot Merkle root hash of the content (optional).
     * @return Result with the created and stored artifact or an error.
     */
    suspend fun publishArtifact(
        registryId: UUID,
        group: String,
        name: String,
        version: String,
        createdBy: UserId,
        merkleRoot: String? = null
    ): Result<Artifact> {
        try {
            val registryResult = registryRepository.findById(registryId)
            if (registryResult.isFailure) {
                return Result.failure(registryResult.exceptionOrNull() ?: IllegalStateException("Failed to retrieve registry $registryId"))
            }
            val registry = registryResult.getOrNull()
                ?: return Result.failure(IllegalArgumentException("Registry with ID '$registryId' not found"))

            val isActive = registryRepository.isRepositoryActive(registryId) // Devuelve Boolean, puede lanzar excepción
            if (!isActive) {
                return Result.failure(IllegalStateException("Registry '${registry.name}' (ID: $registryId) is not active."))
            }

            val coordinates = ArtifactCoordinates(group, name, version)

            val existingArtifactResult = artifactRepository.findByCoordinates(coordinates)
            if (existingArtifactResult.isFailure) {
                return Result.failure(existingArtifactResult.exceptionOrNull() ?: IllegalStateException("Failed to check for existing artifact"))
            }
            if (existingArtifactResult.getOrNull() != null) {
                return Result.failure(IllegalStateException("Artifact ${coordinates.group}:${coordinates.name}:${coordinates.version} already exists"))
            }

            val now = Instant.now()
            val newArtifactId = ArtifactId(java.util.UUID.randomUUID().toString())
            // Asume que Artifact tiene campos como customProperties y updatedAt
            val artifact = Artifact(
                id = newArtifactId,
                coordinates = coordinates,
                createdBy = createdBy,
                createdAt = now,
                updatedAt = now, // Opcional, podría ser igual a createdAt inicialmente
                merkleRoot = merkleRoot?.let { MerkleRootHash(it) },
                customProperties = emptyMap() // Inicializar metadatos si es necesario
            )

            return artifactRepository.save(artifact).fold(
                onSuccess = { savedArtifact ->
                    publishArtifactEvent(
                        ArtifactPublishedEvent(
                            artifactId = savedArtifact.id,
                            publishedAt = now,
                            publishedBy = createdBy
                        )
                    )
                    Result.success(savedArtifact)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            // Captura excepciones de llamadas a repositorios que devuelven Boolean directamente (isRepositoryActive)
            // o de otras operaciones inesperadas.
            return Result.failure(RuntimeException("Failed to publish artifact: ${e.message}", e))
        }
    }

    /**
     * Downloads an artifact.
     * @param artifactId ID of the artifact to download.
     * @param downloadedBy ID of the user downloading (optional).
     * @param clientIp IP of the client downloading (optional).
     * @param userAgent User agent of the client downloading (optional).
     * @return Result with the artifact or an error if not found or the operation fails.
     */
    suspend fun downloadArtifact(
        artifactId: ArtifactId,
        downloadedBy: UserId? = null,
        clientIp: String? = null,
        userAgent: String? = null
    ): Result<Artifact> {
        return artifactRepository.findById(artifactId).fold(
            onSuccess = { artifact ->
                if (artifact == null) {
                    Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found"))
                } else {
                    publishDownloadEvent(
                        ArtifactDownloadedEvent(
                            artifactId = artifact.id,
                            downloadedAt = Instant.now(),
                            downloadedBy = downloadedBy,
                            clientIp = clientIp,
                            userAgent = userAgent
                        )
                    )
                    Result.success(artifact)
                }
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }

    /**
     * Deletes an artifact.
     * @param artifactId ID of the artifact to delete.
     * @param deletedBy ID of the user performing the deletion.
     * @return Result with true if deleted, false if it did not exist, or an error.
     */
    suspend fun deleteArtifact(artifactId: ArtifactId, deletedBy: UserId): Result<Boolean> {
        try {
            val artifactExistsResult = artifactRepository.findById(artifactId)
            if (artifactExistsResult.isFailure) {
                return Result.failure(artifactExistsResult.exceptionOrNull() ?: IllegalStateException("Failed to check artifact existence before deletion"))
            }
            if (artifactExistsResult.getOrNull() == null) {
                // El artefacto no existe, se considera una eliminación "exitosa" en el sentido de que el estado deseado (no existencia) se cumple.
                return Result.success(false)
            }

            val deleted = artifactRepository.deleteById(artifactId) // Devuelve Boolean, puede lanzar excepción
            if (deleted) {
                publishDeleteEvent(
                    ArtifactDeletedEvent(
                        artifactId = artifactId,
                        deletedAt = Instant.now(),
                        deletedBy = deletedBy
                    )
                )
            }
            return Result.success(deleted)
        } catch (e: Exception) {
            // Captura excepciones de llamadas a repositorios que devuelven Boolean directamente (deleteById)
            // o de otras operaciones inesperadas.
            return Result.failure(RuntimeException("Failed to delete artifact ${artifactId.value}: ${e.message}", e))
        }
    }

    /**
     * Gets all versions of a specific artifact by group and name.
     * @param group Group of the artifact.
     * @param name Name of the artifact.
     * @return Result with the list of artifacts or an error.
     */
    suspend fun getAllVersions(
        group: String,
        name: String
    ): Result<List<Artifact>> {
        return artifactRepository.findArtifacts(
            groupFilter = group,
            nameFilter = name
        ) // findArtifacts ya devuelve Result<List<Artifact>>
    }

    /**
     * Gets a specific artifact by its coordinates (group, name, version).
     * If the version is not specified, it returns the latest version.
     * @param group Group of the artifact.
     * @param name Name of the artifact.
     * @param version Version of the artifact (optional).
     * @return Result with the artifact (or null if not found and the latest version was searched) or an error.
     */
    suspend fun getArtifact(
        group: String,
        name: String,
        version: String? = null
    ): Result<Artifact?> {
        return if (version != null) {
            val coordinates = ArtifactCoordinates(group, name, version)
            artifactRepository.findByCoordinates(coordinates)
        } else {
            getAllVersions(group, name).map { artifacts ->
                artifacts.maxByOrNull { it.createdAt } // O it.updatedAt si se prefiere la última modificación
            }
        }
    }

    /**
     * Actualiza los metadatos de un artefacto.
     * Asume que la clase Artifact tiene `customProperties: Map<String, String>` y `updatedAt: Instant?`.
     * Los metadatos proporcionados se fusionan con los existentes.
     * @param artifactId ID del artefacto a actualizar.
     * @param metadata Mapa con los metadatos a añadir o actualizar.
     * @param updatedBy ID del usuario que realiza la actualización.
     * @return Result con el artefacto actualizado o un error.
     */
    suspend fun updateArtifactMetadata(
        artifactId: ArtifactId,
        metadata: Map<String, String>,
        updatedBy: UserId
    ): Result<Artifact> {
        return artifactRepository.findById(artifactId).fold(
            onSuccess = { artifact ->
                if (artifact == null) {
                    Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found for metadata update."))
                } else {
                    // Asumiendo que Artifact es una data class y tiene customProperties y updatedAt
                    // y que customProperties es Map<String, String>
                    val newCustomProperties =
                        artifact.customProperties?.plus(metadata) // Fusiona los mapas, los nuevos valores sobrescriben los antiguos
                    val now = Instant.now()
                    val updatedArtifact = artifact.copy(
                        customProperties = newCustomProperties,
                        updatedAt = now
                    )

                    artifactRepository.save(updatedArtifact).fold(
                        onSuccess = { savedArtifact ->
                            publishMetadataUpdateEvent(
                                ArtifactMetadataUpdatedEvent(
                                    artifactId = savedArtifact.id,
                                    updatedAt = now, // Usar el 'now' consistente con la actualización
                                    updatedBy = updatedBy,
                                    updatedMetadata = metadata // Se envían los metadatos que se intentaron actualizar
                                )
                            )
                            Result.success(savedArtifact)
                        },
                        onFailure = { exception -> Result.failure(exception) }
                    )
                }
            },
            onFailure = { exception -> Result.failure(exception) }
        )
    }
}