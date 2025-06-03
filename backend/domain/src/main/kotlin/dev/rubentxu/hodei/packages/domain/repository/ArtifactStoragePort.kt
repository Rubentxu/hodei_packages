package dev.rubentxu.hodei.packages.domain.repository

import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import java.io.InputStream
import java.io.OutputStream

/**
 * Port for interacting with the physical storage of artifact binaries.
 * This interface abstracts the underlying storage mechanism (e.g., local filesystem, S3).
 */
interface ArtifactStoragePort {

    /**
     * Stores the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to store.
     * @param inputStream The InputStream containing the binary data of the artifact.
     * @return The path or identifier where the artifact is stored, or null if storage failed.
     */
    suspend fun storeArtifact(artifact: Artifact, inputStream: InputStream): String?

    /**
     * Retrieves the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to retrieve.
     * @param outputStream The OutputStream to write the artifact's binary data to.
     * @return True if the artifact was successfully retrieved and written to the outputStream, false otherwise.
     */
    suspend fun retrieveArtifact(artifact: Artifact, outputStream: OutputStream): Boolean

    /**
     * Deletes the binary data of an artifact.
     *
     * @param artifact The metadata of the artifact to delete.
     * @return True if the artifact was successfully deleted, false otherwise.
     */
    suspend fun deleteArtifact(artifact: Artifact): Boolean

    /**
     * Checks if an artifact's binary data exists in storage.
     *
     * @param artifact The metadata of the artifact to check.
     * @return True if the artifact binary exists, false otherwise.
     */
    suspend fun artifactExists(artifact: Artifact): Boolean

    /**
     * Gets the storage path for a given artifact.
     * This path is relative to the storage root and typically includes repository and artifact coordinates.
     * Example: "my-maven-repo/com/example/my-lib/1.0.0/my-lib-1.0.0.jar"
     *
     * @param artifact The artifact for which to determine the storage path.
     * @return The relative storage path as a String.
     */
    fun getStoragePath(artifact: Artifact): String
}
