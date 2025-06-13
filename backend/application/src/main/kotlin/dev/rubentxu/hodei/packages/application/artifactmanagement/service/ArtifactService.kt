package dev.rubentxu.hodei.packages.application.artifactmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.command.UploadArtifactCommand
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactMetadata
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactStatus
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.artifactmanagement.service.ArtifactServicePort
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.DeploymentPolicy
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.HostedRegistry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.Registry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RepositoryType
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.service.StorageService
import java.time.Instant
import java.util.UUID

/**
 * Domain service that encapsulates business logic related to artifact management.
 * This service coordinates operations between artifacts and repositories, and emits domain events.
 * It relies on [dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository] for artifact persistence, [dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository] for registry metadata,
 * [StorageService] for content operations, and [dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler]s for type-specific logic.
 *
 * @property artifactRepository Port for artifact data persistence.
 * @property registryRepository Port for registry metadata persistence.
 * @property storageService Port for artifact content storage and hashing.
 * @property handlers A map of [dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType] to their respective [dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler] implementations.
 * @property publishArtifactEvent Callback to publish an [dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent].
 * @property publishDownloadEvent Callback to publish an [dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent].
 * @property publishMetadataUpdateEvent Callback to publish an [dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent].
 * @property publishDeleteEvent Callback to publish an [dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent].
 */
class ArtifactService(
    private val artifactRepository: ArtifactRepository,
    private val registryRepository: RegistryRepository,
    private val storageService: StorageService,
    private val handlers: Map<ArtifactType, FormatHandler>,
    private val publishArtifactEvent: (ArtifactPublishedEvent) -> Unit,
    private val publishDownloadEvent: (ArtifactDownloadedEvent) -> Unit,
    private val publishMetadataUpdateEvent: (ArtifactMetadataUpdatedEvent) -> Unit,
    private val publishDeleteEvent: (ArtifactDeletedEvent) -> Unit
): ArtifactServicePort {

    override suspend fun uploadArtifact(
        command: UploadArtifactCommand // Asumiendo que UploadArtifactCommand tiene:
                                      // registryId: RegistryId, filename: String, content: ByteArray,
                                      // artifactType: ArtifactType,
                                      // providedUserMetadata: Map<String, String>?, (nuevo nombre y tipo)
                                      // uploaderUserId: UserId (nuevo nombre)
    ): Result<Artifact> {
        try {
            // 1. Validate Registry
            val registry = registryRepository.findById(command.registryId).getOrElse {
                return Result.failure(
                    it ?: IllegalStateException("Failed to retrieve registry ${command.registryId} due to an unknown error.")
                )
            } ?: return Result.failure(IllegalArgumentException("Registry with ID '${command.registryId}' not found."))

            // Asumiendo que registryRepository.isRepositoryActive existe y es relevante
            if (!registryRepository.isRepositoryActive(command.registryId)) {
                return Result.failure(IllegalStateException("Registry '${registry.name}' (ID: ${command.registryId}) is not active."))
            }

            // 1.1 Validate Repository Policy
            validateRepositoryPolicy(
                registry,
                command.filename,
                command.artifactType
            ).getOrElse { return Result.failure(it) }


            // 2. Format Handling
            val handler = handlers[command.artifactType]
                ?: return Result.failure(UnsupportedOperationException("ArtifactType '${command.artifactType}' not supported by any registered FormatHandler."))

            val extractCoordinatesResult = handler.extractCoordinates(command.filename, command.content, command.providedUserMetadata)
            val (parsedCoordinates, _) = extractCoordinatesResult.getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to parse artifact coordinates from '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }

            val determinePackagingTypeResult = handler.determinePackagingType(command.filename, command.content)
            val (packagingType, _) = determinePackagingTypeResult.getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to determine packaging type for '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }

            // 3. Content Hashing & Size
            // Manteniendo la lógica original de storageService. Si StorageService.storeContent(ByteArray) devuelve ContentHash y almacena,
            // esta sección se simplificaría.
            val contentHash = storageService.calculateHash(command.content)
            val sizeInBytes = command.content.size.toLong()

            // 4. Check for existing artifact by coordinates
            artifactRepository.findByCoordinates(parsedCoordinates).fold(
                onSuccess = { existingArtifact ->
                    if (existingArtifact != null) {
                        return Result.failure(IllegalStateException("Artifact ${parsedCoordinates.toString()} already exists."))
                    }
                },
                onFailure = { return Result.failure(it) }
            )

            // 5. Create Artifact instance
            val artifactId = ArtifactId(UUID.randomUUID().toString()) // O alguna otra forma de generar ArtifactId

            val extractMetadataResult = handler.extractMetadataWithSources(
                filename = command.filename,
                content = command.content,
                providedMetadata = command.providedUserMetadata,
                artifactId = artifactId,
                userId = command.uploaderUserId
            )
            val artifactMetadataWithSources = extractMetadataResult.getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to extract metadata for '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }
            val finalMetadata = artifactMetadataWithSources.toArtifactMetadata()

            val extractDependenciesResult = handler.extractDependencies(command.content)
            val dependencies = extractDependenciesResult.getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to extract dependencies for '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }

            @Suppress("UNCHECKED_CAST")
            val tags = artifactMetadataWithSources.additionalMetadata["tags"]?.value as? List<String>

            val artifact = Artifact(
                id = artifactId,
                contentHash = contentHash,
                coordinates = parsedCoordinates,
                tags = tags,
                packagingType = packagingType,
                sizeInBytes = sizeInBytes, // Usar el tamaño real del contenido. finalMetadata.sizeInBytes es el de metadatos.
                status = ArtifactStatus.ACTIVE, // O determinar según política/comando
                metadata = finalMetadata,
                dependencies = dependencies.takeIf { it.isNotEmpty() }
            )

            // 6. Store Content physically
            storageService.store(command.content).getOrElse { // Manteniendo la lógica original de store(ByteArray)
                return Result.failure(
                    RuntimeException(
                        "Failed to store artifact content for ${artifact.coordinates}: ${it.message}",
                        it
                    )
                )
            }

            // 7. Save Artifact Metadata
            return artifactRepository.save(artifact).fold(
                onSuccess = { savedArtifact ->
                    publishArtifactEvent(
                        ArtifactPublishedEvent(
                            artifactId = savedArtifact.id,
                            publishedAt = Instant.now(),
                            publishedBy = command.uploaderUserId
                        )
                    )
                    Result.success(savedArtifact)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )

        } catch (e: Exception) {
            return Result.failure(RuntimeException("Failed to upload artifact '${command.filename}': ${e.message}", e))
        }
    }

    private suspend fun validateRepositoryPolicy(
        registry: Registry,
        filename: String,
        artifactType: ArtifactType
    ): Result<Unit> {
        if (registry.type == RepositoryType.PROXY || registry.type == RepositoryType.GROUP) {
            return Result.failure(UnsupportedOperationException("Direct upload not allowed to ${registry.type} repositories."))
        }

        if (registry is HostedRegistry && registry.deploymentPolicy == DeploymentPolicy.READ_ONLY) {
            return Result.failure(IllegalStateException("Repository '${registry.name}' is read-only."))
        }

        // More sophisticated format compatibility check
        if (registry.format != ArtifactType.GENERIC && registry.format != artifactType) {
            // If the registry format is specific (not GENERIC) and doesn't match the artifact type
            return Result.failure(IllegalArgumentException("Artifact type '$artifactType' is not allowed in repository '${registry.name}' which is of format '${registry.format}'."))
        }
        // If registry.format is GENERIC, it implicitly allows any artifactType (or at least ArtifactType.GENERIC).
        // If registry.format matches artifactType, it's also allowed.

        return Result.success(Unit)
    }


    /**
     * Records the download of an artifact and returns the artifact details (metadata).
     * To get the actual content, use `retrieveArtifactContent`.
     *
     * @param artifactId The [ArtifactId] of the artifact to download.
     * @param downloadedBy The optional [dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId] of the user downloading the artifact.
     * @param clientIp The optional IP address of the client downloading the artifact.
     * @param userAgent The optional user agent string of the client.
     * @return A [Result] containing the [Artifact] metadata if found, or an error.
     */
    override suspend fun downloadArtifact(
        artifactId: ArtifactId,
        downloadedBy: UserId?,
        clientIp: String?,
        userAgent: String?
    ): Result<Artifact> {
        return artifactRepository.findById(artifactId).fold(
            onSuccess = { artifact ->
                if (artifact == null) {
                    Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found."))
                } else {
                    if (artifact.status != ArtifactStatus.ACTIVE && artifact.status != ArtifactStatus.PRE_RELEASE) {
                        return Result.failure(IllegalStateException("Artifact '${artifact.coordinates}' is not in an active/pre-release state (status: ${artifact.status}). Download not allowed."))
                    }
                    // TODO: Check repository online status via registryRepository.isRepositoryActive(registryId_from_artifact_or_context)

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
     * Retrieves the actual content of an artifact.
     *
     * @param registryId The [dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId] context (used for authorization/policy checks).
     * @param contentHash The [dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash] of the artifact to retrieve.
     * @return A [Result] containing the [ByteArray] content if successful, or an error.
     */
    override suspend fun retrieveArtifactContent(
        registryId: RegistryId,
        contentHash: ContentHash
    ): Result<ByteArray> {
        val registry = registryRepository.findById(registryId).getOrElse {
            return Result.failure(
                it ?: IllegalStateException("Failed to retrieve registry $registryId for content retrieval.")
            )
        }
            ?: return Result.failure(IllegalArgumentException("Registry with ID '$registryId' not found for content retrieval."))

        // Asumiendo que registryRepository.isRepositoryActive existe y es relevante
        if (!registryRepository.isRepositoryActive(registryId)) {
            return Result.failure(IllegalStateException("Registry '${registry.name}' (ID: $registryId) is not active for content retrieval."))
        }

        return storageService.retrieve(contentHash).fold( // Asumiendo storageService.retrieve(ContentHash)
            onSuccess = { contentBytes -> Result.success(contentBytes) },
            onFailure = { exception ->
                Result.failure(
                    RuntimeException(
                        "Failed to retrieve content for hash $contentHash: ${exception.message}",
                        exception
                    )
                )
            }
        )
    }


    /**
     * Deletes an artifact from the system.
     * This typically means deleting its metadata. The physical content might be garbage collected later
     * if no other artifacts reference it (due to content-addressable storage).
     *
     * @param artifactId The [ArtifactId] of the artifact to delete.
     * @param deletedBy The [UserId] of the user performing the deletion.
     * @return A [Result] containing `true` if the artifact was successfully deleted,
     *         `false` if it did not exist (idempotent), or an error.
     */
    override suspend fun deleteArtifact(artifactId: ArtifactId, deletedBy: UserId): Result<Boolean> {
        val findResult = artifactRepository.findById(artifactId)
        val artifactToDelete = findResult.getOrElse { return Result.failure(it) }

        if (artifactToDelete == null) {
            return Result.success(false) // Idempotent: artifact not found, considered "deleted"
        }
        // TODO: Add policy checks from the repository before allowing deletion.

        return artifactRepository.deleteById(artifactId).fold(
            onSuccess = { deleted ->
                if (deleted) {
                    publishDeleteEvent(
                        ArtifactDeletedEvent(
                            artifactId = artifactId,
                            deletedAt = Instant.now(),
                            deletedBy = deletedBy
                        )
                    )
                }
                Result.success(deleted)
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }


    /**
     * Retrieves all versions of a specific artifact, identified by its group and name.
     *
     * @param group The group identifier of the artifact (string value).
     * @param name The name of the artifact.
     * @return A [Result] containing a list of [Artifact] objects for all versions, or an error.
     */
    override suspend fun getAllVersions(
        group: String,
        name: String
    ): Result<List<Artifact>> {
        return artifactRepository.findArtifacts(
            groupFilter = group,
            nameFilter = name
        )
    }

    /**
     * Retrieves a specific artifact by its fully qualified coordinates.
     *
     * @param artifactCoordinates The complete coordinates of the artifact to retrieve.
     *                            It is expected that these coordinates include a specific version.
     * @return A [Result] containing the [Artifact] if found, or `null` if not found,
     *         or an error if the lookup fails.
     */
    override suspend fun getArtifact(
        artifactCoordinates: ArtifactCoordinates
    ): Result<Artifact?> {
        return artifactRepository.findByCoordinates(artifactCoordinates)
    }

    /**
     * Updates the metadata of an existing artifact.
     * The provided [newMetadataValues] object should contain the desired new state for the
     * descriptive fields of the artifact's metadata.
     * Immutable fields like original `createdBy` and `createdAt` are preserved from the existing record.
     *
     * @param artifactId The [ArtifactId] of the artifact to update.
     * @param newMetadataValues An [dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactMetadata] object containing the new values for the descriptive metadata fields.
     *                          The `id`, `createdBy`, and `createdAt` fields within this object should ideally match the
     *                          existing ones or will be effectively ignored in favor of the persisted values for those fields.
     * @param updatedBy The [UserId] of the user performing the update, used for eventing.
     * @return A [Result] containing the updated [Artifact] if successful, or an error.
     */
    override suspend fun updateArtifactMetadata(
        artifactId: ArtifactId,
        newMetadataValues: ArtifactMetadata,
        updatedBy: UserId
    ): Result<Artifact> {
        return artifactRepository.findById(artifactId).fold(
            onSuccess = { artifact ->
                if (artifact == null) {
                    Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found for metadata update."))
                } else {
                    // TODO: Policy check: Can this artifact's metadata be updated?
                    val now = Instant.now()
                    val existingPersistedMetadata = artifact.metadata

                    // Create the updated ArtifactMetadata, preserving immutable fields from the existing one
                    // and taking descriptive fields from newMetadataValues.
                    // Note: ArtifactMetadata does not have an 'updatedAt' field.
                    // The 'id', 'createdBy', 'createdAt' from newMetadataValues are ignored here,
                    // as they are taken from the existingPersistedMetadata by the .copy() mechanism.
                    val updatedPersistedMetadata = existingPersistedMetadata.copy(
                        description = newMetadataValues.description,
                        licenses = newMetadataValues.licenses,
                        homepageUrl = newMetadataValues.homepageUrl,
                        repositoryUrl = newMetadataValues.repositoryUrl,
                        sizeInBytes = newMetadataValues.sizeInBytes, // Update size if provided
                        checksums = newMetadataValues.checksums    // Update checksums if provided
                    )

                    val updatedArtifact = artifact.copy(
                        metadata = updatedPersistedMetadata
                        // Consider if other Artifact fields (e.g., tags, status) should be updatable here
                        // or via separate service methods. For now, only metadata is updated.
                    )

                    artifactRepository.save(updatedArtifact).fold(
                        onSuccess = { savedArtifact ->
                            val changesForEvent = buildMap<String, String> {
                                newMetadataValues.description?.let { put("description", it) }
                                newMetadataValues.licenses?.takeIf { it.isNotEmpty() }
                                    ?.let { put("licenses", it.joinToString(",")) }
                                newMetadataValues.homepageUrl?.let { put("homepageUrl", it) }
                                newMetadataValues.repositoryUrl?.let { put("repositoryUrl", it) }
                                newMetadataValues.checksums?.takeIf { it.isNotEmpty() }?.let {
                                    put(
                                        "checksums",
                                        it.entries.joinToString(",") { entry -> "${entry.key}:${entry.value}" })
                                }
                                newMetadataValues.sizeInBytes?.let { put("sizeInBytes", it.toString()) }
                            }

                            publishMetadataUpdateEvent(
                                ArtifactMetadataUpdatedEvent(
                                    artifactId = savedArtifact.id,
                                    updatedAt = now,
                                    updatedBy = updatedBy,
                                    updatedMetadata = changesForEvent
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

    /**
     * Generates a descriptor for a given artifact using the appropriate FormatHandler.
     *
     * @param artifactId The ID of the artifact for which to generate the descriptor.
     * @param artifactType The type of the artifact, used to select the correct handler.
     * @return A [Result] containing the generated descriptor string, or an error.
     */
    override suspend fun generateArtifactDescriptor(artifactId: ArtifactId, artifactType: ArtifactType): Result<String> {
        val artifactResult = artifactRepository.findById(artifactId)
        val artifact = artifactResult.getOrElse {
            return Result.failure(
                it ?: IllegalStateException("Failed to retrieve artifact $artifactId for descriptor generation.")
            )
        }
            ?: return Result.failure(IllegalArgumentException("Artifact with ID '$artifactId' not found for descriptor generation."))

        val handler = handlers[artifactType]
            ?: return Result.failure(UnsupportedOperationException("ArtifactType '$artifactType' not supported for descriptor generation."))

        return handler.generateDescriptor(artifact)
    }
}