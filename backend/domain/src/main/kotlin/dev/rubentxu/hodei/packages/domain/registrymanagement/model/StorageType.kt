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