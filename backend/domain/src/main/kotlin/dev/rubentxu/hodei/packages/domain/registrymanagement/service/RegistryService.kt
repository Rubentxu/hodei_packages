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
 * Domain service that encapsulates business logic related to artifact registry management.
 * This service uses the RegistryRepository port for persistence and emits domain events.
 */
class RegistryService(
    private val registryRepository: RegistryRepository,
    private val eventPublisher: (ArtifactRegistryEvent) -> Unit
) {
    /**
     * Creates a new artifact registry in the system.
     * @param name Name of the artifact registry.
     * @param type Type of the artifact registry (MAVEN, NPM).
     * @param description Description of the artifact registry.
     * @param isPublic Whether the artifact registry is public or private.
     * @param createdBy ID of the user creating the artifact registry.
     * @return Result with the created artifact registry or an error.
     */
    suspend fun createArtifactRegistry(
        name: String,
        type: RegistryType,
        description: String,
        isPublic: Boolean,
        createdBy: UserId
    ): Result<Registry> {
        return try {
            if (registryRepository.existsByName(name)) {
                Result.failure(IllegalStateException("An artifact registry with name '$name' already exists"))
            } else {
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
                    storageType = StorageType.LOCAL // Default or determined by other logic
                )

                registryRepository.save(registry).fold(
                    onSuccess = { savedArtifactRegistry ->
                        eventPublisher(
                            ArtifactRegistryEvent.ArtifactRegistryCreated(
                                registryId = savedArtifactRegistry.id,
                                name = savedArtifactRegistry.name,
                                type = savedArtifactRegistry.type,
                                createdBy = savedArtifactRegistry.createdBy,
                                timestamp = savedArtifactRegistry.createdAt
                            )
                        )
                        Result.success(savedArtifactRegistry)
                    },
                    onFailure = { exception ->
                        Result.failure(exception)
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(RuntimeException("Error creating artifact registry: ${e.message}", e))
        }
    }

    /**
     * Updates an existing artifact registry.
     * @param id UUID of the registry to update.
     * @param description Optional new description.
     * @param isPublic Optional new visibility status.
     * @param updatedBy ID of the user performing the update.
     * @return Result with the updated artifact registry or an error.
     */
    suspend fun updateArtifactRegistry(
        id: UUID,
        description: String? = null,
        isPublic: Boolean? = null,
        updatedBy: UserId
    ): Result<Registry> {
        return registryRepository.findById(id).fold(
            onSuccess = { artifactRegistry ->
                if (artifactRegistry == null) {
                    Result.failure(IllegalArgumentException("ArtifactRegistry with ID '$id' not found"))
                } else {
                    val changes = mutableMapOf<String, Any?>()
                    val updatedRegistry = artifactRegistry.copy(
                        description = description?.also { changes["description"] = it } ?: artifactRegistry.description,
                        isPublic = isPublic?.also { changes["isPublic"] = it } ?: artifactRegistry.isPublic,
                        updatedAt = Instant.now()
                    )

                    if (changes.isEmpty() && description == null && isPublic == null) {
                        // No actual changes were requested beyond what's already there
                        // or only timestamp would change, which might not be a "user update"
                        return@fold Result.success(artifactRegistry)
                    }
                    
                    if (updatedRegistry == artifactRegistry.copy(updatedAt = updatedRegistry.updatedAt) && changes.isEmpty()){
                         // Only timestamp changed, no actual data modification by user
                        return@fold Result.success(artifactRegistry)
                    }


                    registryRepository.save(updatedRegistry).fold(
                        onSuccess = { savedArtifactRegistry ->
                            eventPublisher(
                                ArtifactRegistryEvent.ArtifactRegistryUpdated(
                                    registryId = savedArtifactRegistry.id,
                                    name = savedArtifactRegistry.name,
                                    updatedBy = updatedBy, // Using UserId
                                    timestamp = savedArtifactRegistry.updatedAt,
                                    changes = changes
                                )
                            )
                            Result.success(savedArtifactRegistry)
                        },
                        onFailure = { exception -> Result.failure(exception) }
                    )
                }
            },
            onFailure = { exception -> Result.failure(exception) }
        )
    }

    /**
     * Deletes an artifact registry.
     * @param id UUID of the registry to delete.
     * @param deletedBy ID of the user performing the deletion.
     * @return Result with true if deleted, false if not found, or an error.
     */
    suspend fun deleteArtifactRegistry(id: UUID, deletedBy: UserId): Boolean {
        return try {
            val findResult = registryRepository.findById(id)
            if (findResult.isFailure) {
                return false
            }
            val artifactRegistry = findResult.getOrNull()

            if (artifactRegistry == null) {
                return false // Not found, deletion considered successful in terms of state
            } else {
                val deleted = registryRepository.deleteById(id) // Returns Boolean, can throw
                if (deleted) {
                    eventPublisher(
                        ArtifactRegistryEvent.ArtifactRegistryDeleted(
                            registryId = artifactRegistry.id,
                            name = artifactRegistry.name,
                            deletedBy = deletedBy, // Using UserId
                            timestamp = Instant.now()
                        )
                    )
                }
                return deleted
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Changes the visibility of an artifact registry.
     * @param id UUID of the registry.
     * @param isPublic New visibility status.
     * @param updatedBy ID of the user performing the change.
     * @return Result with the updated artifact registry or an error.
     */
    suspend fun changeArtifactRegistryVisibility(id: UUID, isPublic: Boolean, updatedBy: UserId): Result<Registry> {
        return registryRepository.findById(id).fold(
            onSuccess = { artifactRegistry ->
                if (artifactRegistry == null) {
                    Result.failure(IllegalArgumentException("ArtifactRegistry with ID '$id' not found"))
                } else if (artifactRegistry.isPublic == isPublic) {
                    Result.success(artifactRegistry) // No change needed
                } else {
                    val updatedArtifactRegistry = artifactRegistry.copy(
                        isPublic = isPublic,
                        updatedAt = Instant.now()
                    )
                    registryRepository.save(updatedArtifactRegistry).fold(
                        onSuccess = { savedArtifactRegistry ->
                            eventPublisher(
                                ArtifactRegistryEvent.ArtifactRegistryAccessChanged(
                                    registryId = savedArtifactRegistry.id,
                                    name = savedArtifactRegistry.name,
                                    isPublic = savedArtifactRegistry.isPublic,
                                    updatedBy = updatedBy, // Using UserId
                                    timestamp = savedArtifactRegistry.updatedAt
                                )
                            )
                            Result.success(savedArtifactRegistry)
                        },
                        onFailure = { exception -> Result.failure(exception) }
                    )
                }
            },
            onFailure = { exception -> Result.failure(exception) }
        )
    }

    /**
     * Finds all artifact registries, optionally filtered by type.
     * @param type Optional registry type to filter by.
     * @return Result with a list of artifact registries or an error.
     */
    suspend fun findArtifactRegistries(type: RegistryType? = null): Result<List<Registry>> {
        return registryRepository.findAll(type) // Assumes findAll already returns Result<List<Registry>>
    }

    /**
     * Finds an artifact registry by its ID.
     * @param id UUID of the registry.
     * @return Result with the artifact registry if found (or null), or an error.
     */
    suspend fun findArtifactRegistryById(id: UUID): Result<Registry?> {
        return registryRepository.findById(id) // Assumes findById already returns Result<Registry?>
    }

    /**
     * Finds an artifact registry by its name.
     * @param name Name of the registry.
     * @return Result with the artifact registry if found (or null), or an error.
     */
    suspend fun findArtifactRegistryByName(name: String): Result<Registry?> {
        return registryRepository.findByName(name) // Assumes findByName already returns Result<Registry?>
    }
}
