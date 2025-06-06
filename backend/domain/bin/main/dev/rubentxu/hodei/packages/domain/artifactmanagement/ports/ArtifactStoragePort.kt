package dev.rubentxu.hodei.packages.domain.artifactmanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import java.io.InputStream

/**
 * Port to interact with the physical storage of artifact binaries.
 * This interface abstracts the underlying storage mechanism (e.g., local file system, S3).
 */
interface ArtifactStoragePort {

    /**
     * Stores the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to store.
     * @param inputStream The InputStream containing the binary data of the artifact.
     * @return Result with the path or identifier where the artifact is stored, or an error if storage failed.
     */
    suspend fun storeArtifact(artifact: Artifact, inputStream: InputStream): Result<String>

    /**
     * Retrieves the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to retrieve.
     * @return Result with the binary data as ByteArray, or an error if the retrieval failed.
     */
    suspend fun retrieveArtifact(artifact: Artifact): Result<ByteArray>

    /**
     * Deletes the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to delete.
     * @return Result indicating success or containing an error if the deletion failed.
     */
    suspend fun deleteArtifact(artifact: Artifact): Result<Unit>

    /**
     * Checks if the binary data of an artifact exists in storage.
     *
     * @param artifact The metadata of the artifact to check.
     * @return Result with a boolean indicating whether the binary exists, or an error if the check failed.
     */
    suspend fun artifactExists(artifact: Artifact): Result<Boolean>

    /**
     * Gets the storage path for a given artifact.
     * This path is relative to the storage root and typically includes the repository and artifact coordinates.
     * Example: "my-maven-repo/com/example/my-lib/1.0.0/my-lib-1.0.0.jar"
     *
     * @param artifact The artifact for which to determine the storage path.
     * @return The relative storage path as a String.
     */
    fun getStoragePath(artifact: Artifact): String

    /**
     * Downloads the binary content of an artifact by its ID.
     *
     * @param artifactId The ID of the artifact to retrieve/download.
     * @return Result with the binary data of the artifact as ByteArray, or an error if the download failed.
     */
    suspend fun download(artifactId: String): Result<ByteArray>
} 