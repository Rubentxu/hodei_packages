package dev.rubentxu.hodei.packages.domain.registrymanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ContentHash
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.StorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

/**
 * A file system-based implementation of the [StorageService].
 * This service stores artifact content directly on the local file system.
 *
 * It uses a content-addressable storage approach where files are named by their
 * SHA-256 hash and organized into a sharded directory structure to prevent
 * performance issues with a large number of files in a single directory.
 *
 * For example, a file with hash `abcdef123...` might be stored as:
 * `basePath/ab/cd/ef/abcdef123...` (configurable shard depth and length).
 *
 * @property basePath The root directory where artifact content will be stored.
 * @property shardDepth The number of subdirectory levels for sharding (e.g., 3 means /aa/bb/cc/).
 * @property shardLength The length of each shard directory name (e.g., 2 means /aa/).
 */
class FileStorageService(
    private val basePath: String,
    private val shardDepth: Int = 3, // e.g., 3 levels: /ab/cd/ef
    private val shardLength: Int = 2  // e.g., 2 chars per level: /ab
) : StorageService {

    init {
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw IOException("Failed to create base storage directory: $basePath")
            }
        }
        if (!baseDir.isDirectory) {
            throw IllegalArgumentException("Base path is not a directory: $basePath")
        }
    }

    /**
     * Calculates the SHA-256 hash of the given content.
     * This implementation is consistent with the hashing used for storing files.
     *
     * @param content The byte array content to hash.
     * @return The calculated [ContentHash].
     */
    override suspend fun calculateHash(content: ByteArray): ContentHash = withContext(Dispatchers.IO) {
        // This specific hashing logic could also be a private utility if not exposed elsewhere
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        ContentHash(hashString)
    }

    /**
     * Stores the given content into the file system.
     * The file is stored in a sharded directory structure based on its SHA-256 hash.
     * Implements a safer write by first writing to a temporary file and then moving it.
     * If content with the same hash already exists, this operation is idempotent (no re-write).
     *
     * @param content The byte array content to store.
     * @return A [Result] containing the [ContentHash] if successful, or an exception on failure.
     */
    override suspend fun store(content: ByteArray): Result<ContentHash> = withContext(Dispatchers.IO) {
        Result.runCatching {
            val contentHashInstance = calculateHashInternal(content) // Use internal direct calculation
            val targetFile = getFileForHash(contentHashInstance)

            if (targetFile.exists()) {
                // Content already exists (deduplication)
                return@runCatching contentHashInstance
            }

            targetFile.parentFile.mkdirs() // Ensure sharded directories exist

            // Safer write: write to temp file first, then move
            val tempFile = File(targetFile.parentFile, "${contentHashInstance.value}.${UUID.randomUUID()}.tmp")
            try {
                tempFile.writeBytes(content)
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (e: Exception) {
                tempFile.delete() // Clean up temp file on failure
                throw e // Re-throw to be caught by runCatching
            }
            contentHashInstance
        }
    }

    /**
     * Retrieves content for the given [ContentHash] from the file system.
     *
     * @param contentHash The hash of the content to retrieve.
     * @return A [Result] containing the content as a [ByteArray] if successful,
     *         or [NoSuchFileException] if not found, or other [IOException] on errors.
     */
    override suspend fun retrieve(contentHash: ContentHash): Result<ByteArray> = withContext(Dispatchers.IO) {
        Result.runCatching {
            val file = getFileForHash(contentHash)
            if (file.exists() && file.isFile) {
                file.readBytes()
            } else {
                throw NoSuchFileException("File not found for hash: ${contentHash.value} at path: ${file.absolutePath}")
            }
        }
    }

    /**
     * Deletes the content associated with the given [ContentHash] from the file system.
     * Also attempts to clean up empty parent shard directories.
     *
     * @param contentHash The hash of the content to delete.
     * @return `true` if the content was deleted or did not exist, `false` on failure.
     */
    override suspend fun delete(contentHash: ContentHash): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getFileForHash(contentHash)
            if (!file.exists()) {
                return@withContext true // Idempotent: already deleted or never existed
            }
            val deleted = file.delete()
            if (deleted) {
                cleanupShardDirectories(file.parentFile)
            }
            deleted
        } catch (e: SecurityException) {
            // Log this error appropriately in a real application
            // logger.error("Security exception while deleting ${contentHash.value}", e)
            false
        } catch (e: IOException) {
            // logger.error("IO exception while deleting ${contentHash.value}", e)
            false
        }
    }

    /**
     * Checks if content for the given [ContentHash] exists in the file system.
     *
     * @param contentHash The hash of the content to check.
     * @return `true` if the content exists, `false` otherwise.
     */
    override suspend fun exists(contentHash: ContentHash): Boolean = withContext(Dispatchers.IO) {
        val file = getFileForHash(contentHash)
        file.exists() && file.isFile
    }

    /**
     * Internal utility to calculate SHA-256 hash string directly.
     * This is used by `store` to avoid the suspend function call overhead within its own logic.
     */
    private fun calculateHashInternal(content: ByteArray): ContentHash {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        return ContentHash(hashString)
    }

    /**
     * Constructs the [File] object representing the storage location for a given [ContentHash],
     * including the sharded directory structure.
     *
     * @param contentHash The hash of the content.
     * @return The [File] object pointing to the expected location of the content.
     */
    private fun getFileForHash(contentHash: ContentHash): File {
        var currentPath = File(basePath)
        val hashValue = contentHash.value
        if (hashValue.length < shardDepth * shardLength) {
            // Hash too short for full sharding, place in a special "short_hashes" dir or directly
            // This case should ideally not happen with standard hashes like SHA-256 (64 chars)
            // For simplicity, we'll place it directly in base if too short, but a dedicated handler is better.
            // Or throw an IllegalArgumentException if hashes are expected to be of a certain length.
            // For now, let's assume hashes are long enough.
            if (shardDepth > 0 && shardLength > 0) {
                // Fallback for very short hashes, not ideal for production
                // but prevents IndexOutOfBoundsException
                val shortHashDir = File(basePath, "_short_hashes_")
                if (!shortHashDir.exists()) shortHashDir.mkdirs()
                currentPath = shortHashDir
            }
        } else {
            for (i in 0 until shardDepth) {
                val shardDirName = hashValue.substring(i * shardLength, (i + 1) * shardLength)
                currentPath = File(currentPath, shardDirName)
            }
        }
        return File(currentPath, hashValue)
    }

    /**
     * Recursively cleans up empty parent shard directories after a file deletion.
     *
     * @param directory The directory from which a file was deleted.
     */
    private fun cleanupShardDirectories(directory: File?) {
        var currentDir = directory
        val baseDirFile = File(basePath)

        // Iterate upwards as long as the directory is empty and not the base storage path
        while (currentDir != null && currentDir.isDirectory && currentDir.list()?.isEmpty() == true && currentDir != baseDirFile && currentDir.path.startsWith(baseDirFile.path)) {
            if (currentDir.delete()) {
                currentDir = currentDir.parentFile
            } else {
                // Failed to delete (e.g., permissions, or became non-empty), stop cleanup
                break
            }
        }
    }
}