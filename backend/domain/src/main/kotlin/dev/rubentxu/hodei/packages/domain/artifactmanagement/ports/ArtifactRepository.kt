package dev.rubentxu.hodei.packages.domain.artifactmanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash // Assuming ContentHash is the type for hashes

/**
 * Defines the contract for a repository that manages the persistence of [Artifact] entities.
 * This port abstracts the data access layer for artifact metadata.
 * All operations are designed to be asynchronous.
 */
interface ArtifactRepository {
    /**
     * Saves (creates or updates) an [Artifact] entity in the persistence layer.
     *
     * @param artifact The [Artifact] entity to save.
     * @return A [Result] containing the saved [Artifact] if successful, or an exception on failure.
     */
    suspend fun save(artifact: Artifact): Result<Artifact>

    /**
     * Finds an [Artifact] by its unique identifier.
     *
     * @param id The [ArtifactId] of the artifact to find.
     * @return A [Result] containing the [Artifact] if found, or `null` within [Result.success] if not found.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findById(id: ArtifactId): Result<Artifact?>

    /**
     * Finds an [Artifact] by its content hash.
     *
     * @param contentHash The [ContentHash] of the artifact's content.
     * @return A [Result] containing the [Artifact] if found, or `null` within [Result.success] if not found.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findByContentHash(contentHash: ContentHash): Result<Artifact?>

    /**
     * Finds an [Artifact] by its coordinates (group, artifact name, version).
     *
     * @param coordinates The [ArtifactCoordinates] to search for.
     * @return A [Result] containing the [Artifact] if found, or `null` within [Result.success] if not found.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findByCoordinates(coordinates: ArtifactCoordinates): Result<Artifact?>

    /**
     * Finds all versions for a given group and artifact name.
     * This typically returns a list of version strings.
     *
     * @param groupId The group identifier of the artifact.
     * @param artifactName The name of the artifact.
     * @return A [Result] containing a list of version strings. The list may be empty.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findVersions(groupId: String, artifactName: String): Result<List<String>>

    /**
     * Finds all [Artifact] entities matching the given group and artifact name.
     * This is useful for operations that need the full artifact objects for all versions.
     *
     * @param groupFilter The group identifier to filter by.
     * @param nameFilter The artifact name to filter by.
     * @return A [Result] containing a list of [Artifact] entities. The list may be empty.
     *         Returns [Result.failure] if an error occurs.
     */
    suspend fun findArtifacts(groupFilter: String, nameFilter: String): Result<List<Artifact>>


    /**
     * Deletes an [Artifact] by its unique identifier.
     *
     * @param id The [ArtifactId] of the artifact to delete.
     * @return A [Result] containing `true` if the artifact was successfully deleted,
     *         `false` if the artifact was not found (making the operation idempotent for non-existence).
     *         Returns [Result.failure] if an error occurs during deletion.
     */
    suspend fun deleteById(id: ArtifactId): Result<Boolean>
}