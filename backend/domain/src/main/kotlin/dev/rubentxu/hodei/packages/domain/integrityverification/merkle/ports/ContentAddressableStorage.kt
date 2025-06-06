package dev.rubentxu.hodei.packages.domain.ports.merkle

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash


/**
 * Port for content-addressable storage.
 * Defines the operations for storing and retrieving content using its hash as an identifier.
 * This is a key component for implementing Merkle graphs and integrity verification.
 *
 * Following the principles of hexagonal architecture, this interface acts as a port
 * in the domain model that will be implemented by adapters in the infrastructure layer.
 */
interface ContentAddressableStorage {
    /**
     * Stores content in the system and returns its cryptographic hash.
     *
     * @param content The bytes of the content to store
     * @param algorithm The hashing algorithm to use (default is SHA-256)
     * @return Result encapsulating the hash of the content or an error
     */
    suspend fun store(content: ByteArray, algorithm: String = "SHA-256"): Result<ContentHash>

    /**
     * Retrieves content by its hash.
     *
     * @param contentHash The hash of the content to retrieve
     * @return Result encapsulating the bytes of the content or an error
     */
    suspend fun retrieve(contentHash: ContentHash): Result<ByteArray?>

    /**
     * Checks if content with the specified hash exists in storage.
     *
     * @param contentHash The hash of the content to check
     * @return Result encapsulating true if it exists, false if not, or an error
     */
    suspend fun exists(contentHash: ContentHash): Result<Boolean>

    /**
     * Deletes content from storage by its hash.
     *
     * @param contentHash The hash of the content to delete
     * @return Result encapsulating true if it was deleted, false if it did not exist, or an error
     */
    suspend fun delete(contentHash: ContentHash): Result<Boolean>
}