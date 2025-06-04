package dev.rubentxu.hodei.packages.domain.model.registry

/**
 * Defines the types of storage backends that a repository can use.
 */
enum class StorageType {
    /**
     * The repository stores artifacts on the local filesystem.
     */
    LOCAL,

    /**
     * The repository stores artifacts in an S3-compatible object storage.
     */
    S3
}
