package dev.rubentxu.hodei.packages.domain.registrymanagement.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import java.time.Instant
import java.util.*

/**
 * Represents a unique identifier for a registry.
 * It wraps a UUID to provide strong typing.
 *
 * @property value The UUID representing the registry ID.
 */
@JvmInline
value class RegistryId(val value: UUID) {
    /**
     * Initializes a new instance of the [RegistryId].
     * Ensures the underlying UUID string representation is not blank,
     * although UUID.toString() inherently produces non-blank strings.
     */
    init {
        // UUID.toString() will always be non-blank, but this is a harmless sanity check.
        require(value.toString().isNotBlank()) { "RegistryId's underlying UUID string cannot be blank." }
    }

    /**
     * Returns the string representation of the UUID.
     */
    override fun toString(): String = value.toString()

    companion object {
        /**
         * Creates a new, random [RegistryId].
         * @return A new [RegistryId] instance.
         */
        fun random(): RegistryId = RegistryId(UUID.randomUUID())

        fun global(): RegistryId = RegistryId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

}

/**
 * Defines the type of the repository.
 * - HOSTED: Stores artifacts directly uploaded to it.
 * - PROXY: Serves as a caching proxy for a remote repository.
 * - GROUP: Aggregates multiple repositories under a single URL.
 */
enum class RepositoryType {
    HOSTED, PROXY, GROUP
}

/**
 * Defines the deployment policy for a hosted repository.
 * This controls how artifacts are published and managed.
 */
enum class DeploymentPolicy {
    /** Allows redeploying stable (non-snapshot) versions. */
    ALLOW_REDEPLOY_STABLE,

    /** Allows redeploying snapshot versions only. Stable versions are immutable. */
    ALLOW_REDEPLOY_SNAPSHOT,

    /** Disallows redeploying any version once published. Artifacts are immutable. */
    DISABLE_REDEPLOY,

    /** The repository is read-only; no new deployments are allowed. */
    READ_ONLY
}


/**
 * Configuration for artifact cleanup policies.
 * Defines rules for automatically removing old or excessive artifacts.
 *
 * @property maxVersionsToKeep The maximum number of unique versions to retain for an artifact.
 *                             Null or 0 means no limit.
 * @property retainSnapshotsForDays The number of days to retain snapshot versions.
 *                                  Null or 0 means snapshots are not cleaned up based on age.
 * @property lastDownloadedThresholdDays The number of days an artifact must not have been downloaded
 *                                       to be eligible for cleanup (if other criteria are met).
 *                                       Null means this criterion is not used.
 * @property cleanupCronExpression Cron expression defining when the cleanup job should run.
 */
data class CleanupPolicy(
    val maxVersionsToKeep: Int? = null,
    val retainSnapshotsForDays: Int? = null,
    val lastDownloadedThresholdDays: Int? = null,
    val cleanupCronExpression: String? = null // e.g., "0 0 2 * * ?" (2 AM daily)
)

/**
 * Represents authentication details for a remote repository connection.
 */
sealed interface RemoteAuthentication {
    /** No authentication is used. */
    data object NoAuthentication : RemoteAuthentication

    /**
     * Basic HTTP authentication.
     * @property username The username.
     * @property password The password (should be stored securely, e.g., encrypted or as a secret reference).
     */
    data class BasicAuthentication(val username: String, val passwordSecretKey: String) : RemoteAuthentication
    // NTLM, Token, etc., could be added here

    /**
     * Token-based authentication (e.g., Bearer token).
     * @property tokenSecretKey The secret key referencing the token.
     */
    data class TokenAuthentication(val tokenSecretKey: String) : RemoteAuthentication
}

/**
 * Configuration specific to a PROXY repository.
 *
 * @property remoteUrl The URL of the remote repository to proxy.
 * @property cacheTTLMinutes The Time-To-Live (in minutes) for cached artifacts.
 *                           After this period, the proxy will re-check the remote for updates.
 * @property metadataMaxAgeMinutes The maximum age (in minutes) for cached remote metadata before attempting to refresh it.
 * @property remoteAuthentication Authentication details for accessing the remote repository.
 * @property connectionTimeoutSeconds Timeout (in seconds) for establishing a connection to the remote.
 * @property socketTimeoutSeconds Timeout (in seconds) for waiting for data from the remote after connection.
 * @property maxConnectionAttempts Maximum number of attempts to connect to the remote on failure.
 * @property isBlocked Administratively set to true to temporarily disable proxying from this remote.
 * @property autoBlockUntil If set, the proxy is automatically blocked from trying the remote until this time,
 *                          typically after repeated failures.
 */
data class ProxyConfig(
    val remoteUrl: String,
    val cacheTTLMinutes: Int = 1440, // Default to 24 hours
    val metadataMaxAgeMinutes: Int = 1440, // Default to 24 hours
    val remoteAuthentication: RemoteAuthentication = RemoteAuthentication.NoAuthentication,
    val connectionTimeoutSeconds: Int = 60,
    val socketTimeoutSeconds: Int = 60,
    val maxConnectionAttempts: Int = 3,
    val isBlocked: Boolean = false,
    val autoBlockUntil: Instant? = null
)

/**
 * Configuration specific to a GROUP repository.
 *
 * @property members A list of [RegistryId]s that are part of this group.
 *                   The order might be significant depending on the precedence policy.
 */
data class GroupConfig(
    val members: List<RegistryId>
    // Future: Add memberPrecedencePolicy (e.g., order-based, metadata-based)
)

/**
 * Base interface for all registry types.
 * Provides common properties for all registries.
 *
 * @property id The unique identifier of the registry.
 * @property name The user-defined name of the registry (must be unique).
 * @property type The [RepositoryType] of this registry.
 * @property online Indicates if the registry is currently online and available.
 *                  Administrators can take registries offline for maintenance.
 * @property storageConfig Configuration for the physical storage of artifacts (for HOSTED and PROXY cache).
 * @property cleanupPolicy Optional policy for automatic artifact cleanup.
 * @property description Optional user-provided description for the registry.
 * @property format The specific package format this registry handles (e.g., "maven", "npm", "docker", "generic").
 *                  This is crucial for format-specific logic.
 */
sealed interface Registry {
    val id: RegistryId
    val name: String
    val type: RepositoryType
    val online: Boolean
    val storageConfig: StorageConfig // May be less relevant for GROUP content, but for its config/metadata.
    val cleanupPolicy: CleanupPolicy?
    val description: String?
    val format: ArtifactType // e.g., "maven2", "npm", "docker", "raw"
}

/**
 * Represents a HOSTED repository.
 * Hosted repositories store artifacts directly uploaded by users or build tools.
 *
 * @property deploymentPolicy The policy governing artifact deployments (e.g., redeploy rules).
 * @property specificFormatConfig A map for format-specific configurations,
 *                                e.g., Maven layout policy, NPM scope handling.
 */
data class HostedRegistry(
    override val id: RegistryId,
    override val name: String,
    override val online: Boolean = true,
    override val storageConfig: StorageConfig,
    override val cleanupPolicy: CleanupPolicy? = null,
    override val description: String? = null,
    override val format: ArtifactType = ArtifactType.GENERIC,
    val deploymentPolicy: DeploymentPolicy = DeploymentPolicy.ALLOW_REDEPLOY_SNAPSHOT,
    val specificFormatConfig: Map<String, String>? = null // e.g., for Maven: "layoutPolicy": "STRICT" or "PERMISSIVE"
) : Registry {
    override val type: RepositoryType get() = RepositoryType.HOSTED

    init {
        require(name.isNotBlank()) { "Registry name cannot be blank." }
        require(name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Registry name can only contain alphanumeric characters, hyphens, and underscores."
        }
        description?.let {
            require(it.length <= 255) { "Registry description cannot exceed 255 characters." }
        }
    }
}

/**
 * Represents a PROXY repository.
 * Proxy repositories cache artifacts from a remote repository.
 *
 * @property proxyConfig Configuration details for the remote proxy behavior.
 */
data class ProxyRegistry(
    override val id: RegistryId,
    override val name: String,
    override val online: Boolean = true,
    override val storageConfig: StorageConfig, // Used for caching artifacts
    override val cleanupPolicy: CleanupPolicy? = null, // For cleaning up cached artifacts
    override val description: String? = null,
    override val format: ArtifactType,
    val proxyConfig: ProxyConfig
) : Registry {
    override val type: RepositoryType get() = RepositoryType.PROXY

    init {
        require(name.isNotBlank()) { "Registry name cannot be blank." }
        require(name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Registry name can only contain alphanumeric characters, hyphens, and underscores."
        }
        description?.let {
            require(it.length <= 255) { "Registry description cannot exceed 255 characters." }
        }
    }
}

/**
 * Represents a GROUP repository.
 * Group repositories aggregate content from multiple member repositories (hosted or proxy).
 * They do not store artifacts themselves but provide a unified access point.
 *
 * @property groupConfig Configuration for the group members.
 */
data class GroupRegistry(
    override val id: RegistryId,
    override val name: String,
    override val online: Boolean = true,
    // StorageConfig for a group might be for its own metadata/configuration, not artifact content.
    // Or it could be nullable if a group truly has no storage needs.
    override val storageConfig: StorageConfig,
    override val cleanupPolicy: CleanupPolicy? = null, // Typically not applicable or applies to aggregated metadata
    override val description: String? = null,
    override val format: ArtifactType, // Group format must match member formats or be a compatible superset
    val groupConfig: GroupConfig
) : Registry {
    override val type: RepositoryType get() = RepositoryType.GROUP

    init {
        require(name.isNotBlank()) { "Registry name cannot be blank." }
        require(name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Registry name can only contain alphanumeric characters, hyphens, and underscores."
        }
        description?.let {
            require(it.length <= 255) { "Registry description cannot exceed 255 characters." }
        }
    }
}
