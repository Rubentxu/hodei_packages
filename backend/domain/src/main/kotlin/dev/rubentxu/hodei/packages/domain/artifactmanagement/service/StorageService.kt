package dev.rubentxu.hodei.packages.domain.artifactmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash

/**
 * Defines the contract for a service that handles the physical storage and retrieval of artifact content.
 * Implementations of this service are responsible for managing the underlying storage mechanism
 * (e.g., file system, cloud storage) and ensuring content integrity, typically through content hashing.
 *
 * The service operates on raw byte arrays and uses `ContentHash` as a unique identifier for stored content,
 * enabling content-addressable storage. All I/O-bound operations are designed to be asynchronous.
 */
interface StorageService {

    /**
     * Asynchronously calculates the content hash for the given byte array.
     * The specific hashing algorithm used (e.g., SHA-256) is an implementation detail of the service,
     * but it must be consistent with the hash generated and returned by the `store` method.
     * This allows clients to determine the hash of content before attempting to store it,
     * for example, to check if it already `exists`.
     *
     * @param content The byte array content to hash.
     * @return The calculated [dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash] for the provided content.
     */
    suspend fun calculateHash(content: ByteArray): ContentHash

    /**
     * Asynchronously stores the given byte array content into the storage system.
     * Upon successful storage, it returns a `ContentHash` that uniquely identifies the stored content.
     * This hash should be consistent with what `calculateHash` would produce for the same content.
     * If content with the same hash already exists, implementations might choose to not store it again
     * (deduplication) and simply return the existing `ContentHash`.
     *
     * @param content The byte array representing the artifact content to be stored.
     * @return A [Result] containing the [ContentHash] of the stored content if successful,
     *         or an exception if an error occurred during storage (e.g., I/O error, insufficient space).
     */
    suspend fun store(content: ByteArray): Result<ContentHash>

    /**
     * Asynchronously retrieves the content associated with the given `ContentHash` from the storage system.
     *
     * @param contentHash The unique hash identifying the content to be retrieved.
     * @return A [Result] containing the byte array of the retrieved content if successful,
     *         or an exception if an error occurred (e.g., content not found for the given hash, I/O error).
     *         A common exception for "not found" would be `java.nio.file.NoSuchFileException` or a custom equivalent.
     */
    suspend fun retrieve(contentHash: ContentHash): Result<ByteArray>

    /**
     * Asynchronously deletes the content associated with the given `ContentHash` from the storage system.
     * If the content does not exist, this method should ideally complete without error, returning `false`
     * or `true` depending on whether an actual deletion occurred or if the item was already absent.
     *
     * @param contentHash The unique hash identifying the content to be deleted.
     * @return `true` if the content was successfully deleted (or was already absent and considered a success),
     *         `false` if deletion failed due to an error (e.g., permissions, I/O error) or if the
     *         implementation chooses to indicate failure for non-existent items.
     *         Note: The exact behavior for non-existent items might vary by implementation.
     */
    suspend fun delete(contentHash: ContentHash): Boolean

    /**
     * Asynchronously checks if content associated with the given `ContentHash` exists in the storage system.
     *
     * @param contentHash The unique hash identifying the content to check for.
     * @return `true` if content with the specified hash exists, `false` otherwise.
     */
    suspend fun exists(contentHash: ContentHash): Boolean
}