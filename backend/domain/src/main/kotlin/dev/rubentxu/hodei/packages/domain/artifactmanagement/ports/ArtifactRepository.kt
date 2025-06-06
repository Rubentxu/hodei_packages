package dev.rubentxu.hodei.packages.domain.artifactmanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId

/**
 * Port for artifact persistence and retrieval.
 * Defines the operations that any artifact repository implementation must support.
 */
interface ArtifactRepository {
    /**
     * Saves or updates an artifact.
     * @param artifact The artifact to save.
     * @return Result with the saved artifact (possibly with updated fields such as generated IDs) or an error.
     */
    suspend fun save(artifact: Artifact): Result<Artifact>

    /**
     * Finds an artifact by its ID.
     * @param id ID of the artifact to search for.
     * @return Result with the found artifact, or null if it does not exist, or an error.
     */
    suspend fun findById(id: ArtifactId): Result<Artifact?>

    /**
     * Finds an artifact by its coordinates (group, name, version).
     * @param coordinates The coordinates of the artifact (group:name:version).
     * @return Result with the found artifact, or null if it does not exist, or an error.
     */
    suspend fun findByCoordinates(coordinates: ArtifactCoordinates): Result<Artifact?>

    /**
     * Deletes an artifact by its ID.
     * @param id ID of the artifact to delete.
     * @return true if successfully deleted, false if it did not exist. Throws exception on underlying access error.
     */
    suspend fun deleteById(id: ArtifactId): Boolean

    /**
     * Lists artifacts that meet certain search criteria.
     * @param groupFilter Optional filter by group.
     * @param nameFilter Optional filter by artifact name.
     * @param limit Maximum number of results to return.
     * @param offset Offset for pagination.
     * @return Result with the list of artifacts that meet the criteria or an error.
     */
    suspend fun findArtifacts(
        groupFilter: String? = null,
        nameFilter: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Artifact>>
}