package dev.rubentxu.hodei.packages.domain.artifactmanagement.service


import dev.rubentxu.hodei.packages.domain.artifactmanagement.command.UploadArtifactCommand
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.StorageService
import java.time.Instant


/**
 * Domain service that encapsulates business logic related to artifact management.
 * This service coordinates operations between artifacts and repositories, and emits domain events.
 * It relies on [ArtifactRepository] for artifact persistence, [RegistryRepository] for registry metadata,
 * [StorageService] for content operations, and [FormatHandler]s for type-specific logic.
 *
 * @property artifactRepository Port for artifact data persistence.
 * @property registryRepository Port for registry metadata persistence.
 * @property storageService Port for artifact content storage and hashing.
 * @property handlers A map of [ArtifactType] (o RegistryFormat) to their respective [FormatHandler] implementations.
 * @property publishArtifactEvent Callback to publish an [ArtifactPublishedEvent].
 * @property publishDownloadEvent Callback to publish an [ArtifactDownloadedEvent].
 * @property publishMetadataUpdateEvent Callback to publish an [ArtifactMetadataUpdatedEvent].
 * @property publishDeleteEvent Callback to publish an [ArtifactDeletedEvent].
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
) {

    suspend fun uploadArtifact(
        command: UploadArtifactCommand // Argumento único
    ): Result<Artifact> {
        try {
            // 1. Validate Registry
            val registry = registryRepository.findById(command.registryId).getOrElse {
                return Result.failure(
                    it
                        ?: IllegalStateException("Failed to retrieve registry ${command.registryId} due to an unknown error.")
                )
            } ?: return Result.failure(IllegalArgumentException("Registry with ID '${command.registryId}' not found."))

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

            val parsedCoordinates = handler.parseCoordinates(command.filename, command.content).getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to parse artifact coordinates from '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }
            // handlerParsedRawMetadata se usa para campos que no están en ArtifactMetadata (ej. tags)
            val handlerParsedRawMetadata =
                handler.parseMetadata(command.content).getOrElse { // Devuelve Map<String, String>
                    return Result.failure(
                        IllegalArgumentException(
                            "Failed to parse metadata from content for '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                            it
                        )
                    )
                }
            val packagingType = handler.getPackagingType(command.filename, command.content).getOrElse {
                return Result.failure(
                    IllegalArgumentException(
                        "Failed to determine packaging type for '${command.filename}' using handler for ${command.artifactType}: ${it.message}",
                        it
                    )
                )
            }

            // 3. Content Hashing & Size
            val contentHash = storageService.calculateHash(command.content)
            val sizeInBytes = command.content.size.toLong()

            // 4. Check for existing artifact by coordinates
            artifactRepository.findByCoordinates(parsedCoordinates).fold(
                onSuccess = { existingArtifact ->
                    if (existingArtifact != null) {
                        val coordsString =
                            "${parsedCoordinates.group.value}:${parsedCoordinates.name}:${parsedCoordinates.version.value}"
                        return Result.failure(IllegalStateException("Artifact $coordsString already exists."))
                    }
                },
                onFailure = { return Result.failure(it) }
            )

            // 5. Create Artifact instance
            val artifactId = ArtifactId(java.util.UUID.randomUUID().toString())

            // Utiliza command.providedMetadata, pero asegura que los campos controlados por el servicio (id, createdBy, timestamps)
            // sean establecidos correctamente por el servicio.
            val initialArtifactMetadata = command.providedMetadata.copy(
                id = artifactId,
                createdBy = command.createdBy,
                createdAt = Instant.now(),
                updatedAt = Instant.now(), // En la creación, updatedAt es igual a createdAt
                // El sizeInBytes en metadata puede tomarse del comando o del contenido real.
                // Es preferible usar el tamaño real del contenido.
                sizeInBytes = sizeInBytes
            )

            val artifact = Artifact(
                id = artifactId,
                contentHash = contentHash,
                coordinates = parsedCoordinates,
                // Los tags se pueden extraer del mapa parseado por el handler si no son parte de ArtifactMetadata
                tags = handlerParsedRawMetadata["tags"]?.split(',')?.map { it.trim() },
                packagingType = packagingType,
                sizeInBytes = sizeInBytes, // Tamaño real del contenido
                status = ArtifactStatus.ACTIVE,
                metadata = initialArtifactMetadata, // El objeto ArtifactMetadata construido
                dependencies = null // Las dependencias podrían parsearse del handlerParsedRawMetadata
            )

            // 6. Store Content physically
            storageService.store(command.content).getOrElse {
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
                            publishedBy = command.createdBy
                        )
                    )
                    Result.success(savedArtifact)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )

        } catch (e: Exception) {
            return Result.failure(RuntimeException("Failed to upload artifact: ${e.message}", e))
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
     * @param downloadedBy The optional [UserId] of the user downloading the artifact.
     * @param clientIp The optional IP address of the client downloading the artifact.
     * @param userAgent The optional user agent string of the client.
     * @return A [Result] containing the [Artifact] metadata if found, or an error.
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
     * @param registryId The [RegistryId] context (used for authorization/policy checks).
     * @param contentHash The [ContentHash] of the artifact to retrieve.
     * @return A [Result] containing the [ByteArray] content if successful, or an error.
     */
    suspend fun retrieveArtifactContent(
        registryId: RegistryId,
        contentHash: ContentHash
    ): Result<ByteArray> {
        val registry = registryRepository.findById(registryId).getOrElse {
            return Result.failure(
                it ?: IllegalStateException("Failed to retrieve registry $registryId for content retrieval.")
            )
        }
            ?: return Result.failure(IllegalArgumentException("Registry with ID '$registryId' not found for content retrieval."))

        if (!registryRepository.isRepositoryActive(registryId)) {
            return Result.failure(IllegalStateException("Registry '${registry.name}' (ID: $registryId) is not active for content retrieval."))
        }

        return storageService.retrieve(contentHash).fold(
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
    suspend fun deleteArtifact(artifactId: ArtifactId, deletedBy: UserId): Result<Boolean> {
        val findResult = artifactRepository.findById(artifactId)
        val artifactToDelete = findResult.getOrElse { return Result.failure(it) }

        if (artifactToDelete == null) {
            return Result.success(false)
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
    suspend fun getAllVersions(
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
    suspend fun getArtifact(
        artifactCoordinates: ArtifactCoordinates
    ): Result<Artifact?> {
        // The ArtifactCoordinates object is expected to have a specific version
        // due to the constructor constraints of ArtifactVersion.
        // This method now directly fetches an artifact by its exact coordinates.
        return artifactRepository.findByCoordinates(artifactCoordinates)
    }

    /**
     * Updates the metadata of an existing artifact.
     * The provided [newMetadataValues] object should contain the desired new state for the
     * descriptive fields of the artifact's metadata.
     * Immutable fields like original `createdBy` and `createdAt` are preserved from the existing record.
     * The `updatedAt` field will be set to the current time.
     *
     * @param artifactId The [ArtifactId] of the artifact to update.
     * @param newMetadataValues An [ArtifactMetadata] object containing the new values for the descriptive metadata fields.
     *                          The `id`, `createdBy`, and `createdAt` fields within this object should ideally match the
     *                          existing ones or will be effectively ignored in favor of the persisted values for those fields.
     * @param updatedBy The [UserId] of the user performing the update, used for eventing.
     * @return A [Result] containing the updated [Artifact] if successful, or an error.
     */
    suspend fun updateArtifactMetadata(
        artifactId: ArtifactId,
        newMetadataValues: ArtifactMetadata, // Parámetro cambiado a ArtifactMetadata
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
                    val updatedPersistedMetadata = existingPersistedMetadata.copy(
                        description = newMetadataValues.description,
                        licenses = newMetadataValues.licenses,
                        homepageUrl = newMetadataValues.homepageUrl,
                        repositoryUrl = newMetadataValues.repositoryUrl,
                        checksums = newMetadataValues.checksums,
                        sizeInBytes = newMetadataValues.sizeInBytes, // Assuming this is the new desired value for metadata's size
                        updatedAt = now // Service sets the update timestamp
                        // id, createdBy, createdAt are implicitly taken from existingPersistedMetadata by .copy()
                        // as they are not specified here, thus preserving their original values.
                    )

                    val updatedArtifact = artifact.copy(
                        metadata = updatedPersistedMetadata
                        // Potentially update other Artifact fields if newMetadataValues implies changes to them,
                        // though this function is primarily for Artifact.metadata.
                    )

                    artifactRepository.save(updatedArtifact).fold(
                        onSuccess = { savedArtifact ->
                            // Construct a map representing the changes or the new state for the event.
                            // This map should only contain non-null String values as per common event practices.
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
                                    updatedAt = now, // Event timestamp
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
    suspend fun generateArtifactDescriptor(artifactId: ArtifactId, artifactType: ArtifactType): Result<String> {
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