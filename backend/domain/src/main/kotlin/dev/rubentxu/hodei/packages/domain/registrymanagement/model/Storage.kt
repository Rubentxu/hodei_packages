package dev.rubentxu.hodei.packages.domain.registrymanagement.model

/**
 * Defines the types of storage backends that a registry can use.
 */
enum class StorageType {
    /**
     * The registry stores artifacts on the local filesystem.
     */
    LOCAL,

    /**
     * The registry stores artifacts in an S3-compatible object storage.
     */
    S3,

    /**
     * The registry stores artifacts in an Azure Blob Storage.
     */
    AZURE,

    /**
     * The registry stores artifacts in a Google Cloud Storage.
     */
    GCP,

    /**
     * The registry stores artifacts in a Kubernetes Volume.
     */
    KUBERNETES_PERSISTENT_VOLUME_CLAIM
}

/**
 * Configuration for the physical storage used by a registry.
 *
 * @property path The base file system path or storage bucket identifier where artifacts are stored.
 * @property blobStoreName The name of the configured blob store to use for this registry.
 * @property strictContentTypeValidation If true, enforces strict validation of content types for uploaded artifacts.
 */
data class StorageConfig(
    val path: String, // This might be deprecated in favor of blobStoreName for more abstraction
    val blobStoreName: String,
    val strictContentTypeValidation: Boolean = true,
    val storageType: StorageType = StorageType.LOCAL // Default to LOCAL if not specified
)
