package dev.rubentxu.hodei.packages.domain.registrymanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.Registry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId
import java.util.UUID

/**
 * Defines the contract for a repository that manages the persistence of [Registry] entities.
 * A [Registry] in this context represents the metadata and configuration of an artifact repository
 * (e.g., a Maven repository, an NPM registry) managed by the system.
 *
 * This port abstracts the data access layer, allowing the domain logic to remain independent
 * of specific database technologies or persistence mechanisms. All operations are designed
 * to be asynchronous.
 */
interface RegistryRepository {

    /**
     * Saves (creates or updates) a [Registry] entity in the persistence layer.
     * If the registry is new, it will be created. If it already exists (e.g., identified by its ID),
     * its state will be updated.
     *
     * @param registry The [Registry] entity to save.
     * @return A [Result] containing the saved [Registry] (which might include generated IDs or updated timestamps)
     *         if successful, or an exception if an error occurred during persistence.
     */
    suspend fun save(registry: Registry): Result<Registry>

    /**
     * Finds a [Registry] by its unique identifier.
     *
     * @param id The [RegistryId] of the registry to find.
     * @return A [Result] containing the [Registry] if found, or `null` within the [Result.success]
     *         if no registry exists with the given ID. Returns [Result.failure] if an error occurs.
     */
    suspend fun findById(id: RegistryId): Result<Registry?>

    /**
     * Finds a [Registry] by its unique name.
     * Registry names are typically expected to be unique within the system.
     *
     * @param name The name of the registry to find.
     * @return A [Result] containing the [Registry] if found, or `null` within the [Result.success]
     *         if no registry exists with the given name. Returns [Result.failure] if an error occurs.
     */
    suspend fun findByName(name: String): Result<Registry?>

    /**
     * Retrieves all [Registry] entities, optionally filtered by their format.
     *
     * @param type An optional [RegistryFormat] to filter the registries by. If `null`, all registries are returned.
     * @return A [Result] containing a list of [Registry] entities matching the criteria.
     *         The list may be empty if no registries are found or match the filter.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findAll(type: ArtifactType): Result<List<Registry>>

    /**
     * Deletes a [Registry] by its unique identifier.
     *
     * @param id The [RegistryId] of the registry to delete.
     * @return A [Result] containing `true` if the registry was successfully deleted,
     *         `false` if the registry was not found (idempotent delete).
     *         Returns [Result.failure] if an error occurs during deletion.
     */
    suspend fun deleteById(id: RegistryId): Result<Boolean>

    /**
     * Checks if a [Registry] with the given name already exists.
     * This is useful for validating uniqueness before creating a new registry.
     *
     * @param name The name to check for existence.
     * @return A [Result] containing `true` if a registry with the name exists, `false` otherwise.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun existsByName(name: String): Result<Boolean>

    /**
     * Checks if a [Registry] identified by its ID is currently active or online.
     * This method assumes the registry exists; prior checks for existence should be done via [findById].
     *
     * @param id The [UUID] of the registry to check.
     * @return `true` if the registry is active/online, `false` otherwise.
     * @throws Exception if an underlying error occurs while trying to determine the status
     *                   (e.g., database connectivity issue).
     */
    suspend fun isRepositoryActive(id: RegistryId): Boolean
}