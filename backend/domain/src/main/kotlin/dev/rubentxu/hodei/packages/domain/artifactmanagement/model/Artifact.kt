package dev.rubentxu.hodei.packages.domain.artifactmanagement.model

import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

// Assuming UserId and ArtifactStatus are defined elsewhere, e.g.:
// @JvmInline value class UserId(val value: String)
// enum class ArtifactStatus { ACTIVE, DEPRECATED, DELETED }

/**
 * Represents a software artifact's core information as stored in the registry.
 * This model focuses on the essential identifying and structural properties of an artifact.
 * For more detailed descriptive metadata, see [ArtifactMetadata].
 *
 * @property id Unique identifier for the artifact within this registry.
 * @property coordinates The logical coordinates (group, name, version, classifier, extension) identifying the artifact.
 * @property tags Optional list of keywords or tags associated with the artifact for discovery.
 * @property packagingType Optional. The specific file format or packaging of the artifact's primary file (e.g., "jar", "tgz", "whl", "crate").
 *                         This describes the physical file format, distinct from `ArtifactCoordinates.extension` which is part of the logical ID.
 * @property sizeInBytes Optional. Size of the artifact's primary file in bytes.
 * @property status The current lifecycle status of the artifact (e.g., ACTIVE, DEPRECATED). Defaults to ACTIVE.
 * @property metadata Optional. A flexible map for storing additional, ecosystem-specific key-value metadata not covered by other fields.
 * @property dependencies Optional. List of logical coordinates of artifacts that this artifact depends on.
 */
data class Artifact(
    val id: ArtifactId,
    val contentHash: ContentHash,
    val coordinates: ArtifactCoordinates,
    val tags: List<String>? = null,
    val packagingType: String? = null,
    val sizeInBytes: Long? = null,
    val status: ArtifactStatus = ArtifactStatus.ACTIVE, // Assuming ArtifactStatus enum is defined
    val metadata: ArtifactMetadata,
    val dependencies: List<ArtifactCoordinates>? = null
)

/**
 * Represents the cryptographic hash of an artifact's content.
 * This ensures data integrity and can be used for deduplication.
 *
 * @property value The hex-encoded string representation of the content hash.
 * @throws IllegalArgumentException if the hash string is blank.
 */
@JvmInline
value class ContentHash(val value: String) {
    init {
        require(value.isNotBlank()) { "Content hash cannot be blank." }
        // You might add more validation here, e.g., regex for hex characters or specific length
        // depending on the hash algorithm(s) you intend to store.
        // For example, if it's always SHA-256:
        // require(value.matches(Regex("^[a-fA-F0-9]{64}$"))) { "Content hash must be a valid SHA-256 hex string." }
    }

    override fun toString(): String = value
}

/**
 * Represents a unique identifier for an artifact in the registry.
 *
 * @property value The string representation of the artifact ID.
 */
@JvmInline
value class ArtifactId(val value: String)

/**
 * Holds detailed descriptive and provenance metadata for an artifact.
 * This information typically complements the core [Artifact] data.
 *
 * @property id The unique identifier of the artifact this metadata belongs to.
 * @property createdBy Identifier of the user or process that created/uploaded this artifact entry. (Assuming UserId type is defined)
 * @property createdAt Timestamp of when this artifact entry was initially created in the registry.
 * @property updatedAt Timestamp of the last update to this metadata. Defaults to `createdAt`.
 * @property description Optional. A brief description of the artifact.
 * @property licenses Optional. List of licenses under which the artifact is distributed (e.g., SPDX IDs).
 * @property homepageUrl Optional. URL to the project's or artifact's homepage.
 * @property repositoryUrl Optional. URL to the source code repository.
 * @property sizeInBytes Optional. Size of the artifact's primary file in bytes, as reported or stored in metadata.
 *                         This might be redundant with [Artifact.sizeInBytes] but can serve as a metadata-sourced value.
 * @property checksums Optional. A map of checksum algorithms to their hex-encoded hash values for the primary artifact file
 *                     (e.g., {"SHA256": "...", "MD5": "..."}).
 */
data class ArtifactMetadata(
    val id: ArtifactId,
    val createdBy: UserId, // Assuming UserId type is defined
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val description: String? = null,
    val licenses: List<String>? = null,
    val homepageUrl: String? = null,
    val repositoryUrl: String? = null,
    val sizeInBytes: Long? = null,
    val checksums: Map<String, String>? = null
)

/**
 * Represents the "group" component of an artifact's coordinates.
 * In many ecosystems, this can be a namespace, organization, or scope.
 *
 * @property value The string value of the group. An empty string (represented by `ArtifactGroup.NONE`)
 *                 indicates that the group concept does not apply or is not specified for this artifact
 *                 in its source ecosystem (e.g., many Python packages on PyPI, Rust crates).
 */
@JvmInline
value class ArtifactGroup(val value: String) {
    companion object {
        /**
         * Represents the absence of a specific group or when it is not applicable.
         * Its value is an empty string.
         */
        val NONE = ArtifactGroup("")
        // You could add constants for common group prefixes if needed,
        // e.g., val NPM_SCOPE_PREFIX = "@"
    }
}

/**
 * Represents the classifier of an artifact.
 * The classifier distinguishes different variants of the same artifact (same GAV: group, artifact, version).
 *
 * @property value The string value of the classifier. An empty string (represented by `ArtifactClassifier.NONE`)
 *                 indicates that there is no specific classifier or that it does not apply to this artifact
 *                 (common in ecosystems like npm, Go, Rust).
 */
@JvmInline
value class ArtifactClassifier(val value: String) {
    companion object {
        /**
         * Represents the absence of a classifier or when it is not applicable.
         * Its value is an empty string.
         */
        val NONE = ArtifactClassifier("")

        // Examples of common classifiers for convenience
        val SOURCES = ArtifactClassifier("sources")
        val JAVADOC = ArtifactClassifier("javadoc")
        // For Kotlin Multiplatform, they could be:
        // val JVM = ArtifactClassifier("jvm")
        // val JS = ArtifactClassifier("js")
        // val METADATA = ArtifactClassifier("metadata")
        // For Python wheels, it could be a concatenation of tags:
        // fun pythonWheel(implementation: String, abi: String, platform: String) = ArtifactClassifier("$implementation-$abi-$platform")
    }
}

/**
 * Represents the file extension of an artifact *as part of its logical coordinates*.
 * This is relevant primarily in ecosystems where the extension forms part of the identifier
 * for dependency resolution (e.g., Maven with "pom").
 * It should not be confused with the `packagingType` of the physical artifact file.
 *
 * @property value The string value of the extension. An empty string (represented by `ArtifactExtension.NONE`)
 *                 indicates that the extension is not part of the artifact's logical coordinates,
 *                 or that the ecosystem's default extension (not explicit in coordinates) is used.
 */
@JvmInline
value class ArtifactExtension(val value: String) {
    companion object {
        /**
         * Represents the absence of a specific extension in the logical coordinates
         * or when it is not applicable. Its value is an empty string.
         */
        val NONE = ArtifactExtension("")

        // Examples of common extensions (when part of logical coordinates)
        val POM = ArtifactExtension("pom")
        val JAR = ArtifactExtension("jar") // Often implicit in Maven, but can be explicit
        // val KLIB = ArtifactExtension("klib") // For Kotlin Multiplatform
    }
}

/**
 * Represents the logical coordinates of an artifact, designed to be generic
 * across different package management ecosystems.
 * All fields are non-null to facilitate consistent identification and hashing.
 * The absence or non-applicability of a component is represented by its respective `NONE` value (empty string).
 *
 * @property group The group ID, namespace, scope, or organization of the artifact.
 *                 E.g., "dev.rubentxu.hodei" (Maven), "@babel" (npm).
 *                 For ecosystems without an explicit group concept (many Python packages, Rust crates),
 *                 `ArtifactGroup.NONE` is used.
 * @property name The name of the artifact. It's the primary identifier within its group/ecosystem.
 *                 E.g., "hodei-core", "core-js", "requests", "serde". Expected to be non-blank.
 * @property version The version of the artifact. E.g., "1.0.0", "4.17.21-alpha.1".
 * @property classifier The classifier of the artifact, used to distinguish variants.
 *                      E.g., "sources" (Maven), "jvm" (Kotlin MP), "cp39-abi3-manylinux_x86_64" (Python Wheel Tags).
 *                      Defaults to `ArtifactClassifier.NONE` if not applicable.
 * @property extension The file extension as part of the logical coordinates (not the physical packaging type).
 *                     Relevant primarily in ecosystems like Maven (e.g., "pom").
 *                     Defaults to `ArtifactExtension.NONE` if not applicable or implied.
 */
data class ArtifactCoordinates(
    val group: ArtifactGroup = ArtifactGroup.NONE,
    val name: String,
    val version: ArtifactVersion,
    val classifier: ArtifactClassifier = ArtifactClassifier.NONE,
    val extension: ArtifactExtension = ArtifactExtension.NONE
) {
    init {
        // It's crucial that the name is not blank, as it's a primary identifier.
        require(name.isNotBlank()) { "Artifact name cannot be blank." }

    }

    /**
     * Generates a canonical string representation for hashing.
     * It's important that this representation is stable and always produces the same output
     * for the same coordinates. It explicitly includes the values of `NONE` components
     * as empty strings.
     *
     * The format is: group.value<DELIMITER>name<DELIMITER>version.value<DELIMITER>classifier.value<DELIMITER>extension.value
     */
    fun toCanonicalStringForHashing(): String {
        // Using a delimiter that is very unlikely to appear in the values.
        // A control character like U+001F (Unit Separator) is a robust option.
        // For simplicity here, if your values are simple, a multi-character delimiter might suffice.
        // Consider the nature of your data when choosing the delimiter.
        val delimiter = ":::" // Or consider U+001F.toString()
        return listOf(
            group.value,
            name,
            version.value,
            classifier.value,
            extension.value
        ).joinToString(delimiter)
    }

    /**
     * Calculates a SHA-256 hash for these ArtifactCoordinates.
     * @return The SHA-256 hash as a hexadecimal string.
     */
    fun sha256(): String {
        val canonicalString = this.toCanonicalStringForHashing()
        val bytes = canonicalString.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        // Convert bytes to hexadecimal string
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Provides a human-readable string representation commonly used for coordinates,
     * omitting "NONE" components for clarity where possible.
     * Format: group:name:version[:classifier][@extension] (empty components are omitted)
     * Note: For a canonical identifier for hashing, it's better to use the direct values
     * including the .value of NONE components (empty strings), as `toCanonicalStringForHashing()` does.
     */
    override fun toString(): String {
        val g = if (group == ArtifactGroup.NONE) "" else "${group.value}:"
        val c = if (classifier == ArtifactClassifier.NONE) "" else ":${classifier.value}"
        // Using @ for extension to visually distinguish it from the classifier in toString()
        // but for canonical hashing, only the value is important.
        val e = if (extension == ArtifactExtension.NONE) "" else "@${extension.value}"


        // Adjustment for npm where the "group" is a scope and joins directly with the name
        return if (group != ArtifactGroup.NONE && group.value.startsWith("@") && !group.value.endsWith("/")) {
            // Assuming an npm scope doesn't end with / and is joined with / to the name
            "${group.value}/${name}:${version.value}${c}${e}"
        } else {
            "${g}${name}:${version.value}${c}${e}"
        }
    }
}

/**
 * Represents the version of an artifact.
 *
 * @property value The version string, e.g., "1.0.0". Must not be blank.
 * @throws IllegalArgumentException if the version string is blank.
 */
@JvmInline
value class ArtifactVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "Artifact version cannot be blank" }
    }

    override fun toString(): String = value
}

/**
 * Represents the lifecycle status of an artifact within the repository.
 */
enum class ArtifactStatus {
    /**
     * The artifact is actively available for use and is considered stable or the latest in its development line (e.g., a released version or an active snapshot).
     * This is the default status for newly published, non-pending artifacts.
     */
    ACTIVE,

    /**
     * The artifact is a pre-release version (e.g., alpha, beta, release candidate).
     * It is available for testing or early access but is not yet considered stable for production.
     * The version string itself often also indicates this (e.g., "1.0.0-beta.1").
     */
    PRE_RELEASE,

    /**
     * The artifact has been uploaded or submitted and is awaiting validation, scanning, or manual approval
     * before becoming `ACTIVE` or potentially `REJECTED`.
     */
    PENDING,

    /**
     * The artifact is still available but is no longer recommended for new use.
     * A newer version or an alternative artifact is typically suggested.
     */
    DEPRECATED,

    /**
     * The artifact is no longer actively maintained or easily discoverable but is retained for historical,
     * compliance, or legacy support reasons. It might be stored in a separate archive repository.
     */
    ARCHIVED,

    /**
     * The artifact has been found to have issues (e.g., critical security vulnerabilities, policy violations)
     * and has been isolated to prevent its download or use. It may be awaiting remediation or a decision.
     */
    QUARANTINED,

    /**
     * The artifact has been explicitly rejected, typically during an approval or validation workflow,
     * or due to failing to meet certain criteria after being `PENDING` or `QUARANTINED`.
     */
    REJECTED,

    /**
     * The artifact has been administratively disabled and is not available for download or use.
     * This might be temporary or a precursor to being `BANNED` or `DELETED`.
     * Differs from `QUARANTINED` as it might not be due to an automated scan.
     */
    DISABLED,

    /**
     * The artifact is strictly prohibited from use or distribution, typically due to severe security vulnerabilities,
     * legal issues, or other critical policy violations. Access is blocked.
     */
    BANNED,

    /**
     * The artifact has been physically or logically removed from the repository.
     * For released artifacts, this action is typically discouraged to avoid breaking downstream consumers.
     */
    DELETED,

    /**
     * The status of the artifact cannot be determined or is in an initial, undefined state.
     * This should ideally be a transient or error state.
     */
    UNKNOWN
}