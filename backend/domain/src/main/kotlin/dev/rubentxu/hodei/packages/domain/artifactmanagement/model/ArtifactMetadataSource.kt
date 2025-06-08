package dev.rubentxu.hodei.packages.domain.artifactmanagement.model

import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId

/**
 * Enumeration defining the possible sources of an artifact metadata.
 * This allows tracking the origin of each metadata for validation and auditing.
 */
enum class MetadataSource {
    /**
     * The metadata was extracted from the artifact's content (e.g., POM.xml, package.json).
     */
    CONTENT_EXTRACTED,

    /**
     * The metadata was inferred from the artifact's filename.
     */
    FILENAME_INFERRED,

    /**
     * The metadata was provided by the user through the API.
     */
    USER_PROVIDED,

    /**
     * The metadata was automatically set by the system.
     */
    SYSTEM_GENERATED,

    /**
     * The metadata was obtained from an external repository (e.g., Maven Central, NPM Registry).
     */
    EXTERNAL_REPOSITORY
}

/**
 * Represents a metadata item with its value and source.
 *
 * @param T The type of the metadata value.
 * @property value The value of the metadata.
 * @property source The source of the metadata.
 */
data class MetadataWithSource<T>(
    val value: T,
    val source: MetadataSource
)

/**
 * Enhanced version of ArtifactMetadata that includes the source of each field for validation.
 *
 * @property id The unique identifier of the artifact.
 * @property createdBy Metadata for the user who created the artifact, including its source.
 * @property createdAt Metadata for the creation timestamp of the artifact, including its source.
 * @property description Optional metadata for the artifact's description, including its source.
 * @property licenses Optional metadata for the artifact's licenses, including its source.
 * @property homepageUrl Optional metadata for the artifact's homepage URL, including its source.
 * @property repositoryUrl Optional metadata for the artifact's repository URL, including its source.
 * @property sizeInBytes Optional metadata for the artifact's size in bytes, including its source.
 * @property checksums Optional metadata for the artifact's checksums (e.g., SHA1, MD5), including its source.
 * @property additionalMetadata A map for any additional format-specific metadata, where each value includes its source.
 */
data class ArtifactMetadataWithSources(
    val id: ArtifactId,
    val createdBy: MetadataWithSource<UserId>,
    val createdAt: MetadataWithSource<java.time.Instant>,
    val description: MetadataWithSource<String?>? = null,
    val licenses: MetadataWithSource<List<String>?>? = null,
    val homepageUrl: MetadataWithSource<String?>? = null,
    val repositoryUrl: MetadataWithSource<String?>? = null,
    val sizeInBytes: MetadataWithSource<Long?>? = null,
    val checksums: MetadataWithSource<Map<String, String>?>? = null,
    // Additional format-specific metadata
    val additionalMetadata: Map<String, MetadataWithSource<Any?>> = emptyMap()
) {
    /**
     * Converts this instance to a standard ArtifactMetadata, stripping out the source information.
     * @return An [ArtifactMetadata] instance.
     */
    fun toArtifactMetadata(): ArtifactMetadata {
        return ArtifactMetadata(
            id = id,
            createdBy = createdBy.value,
            createdAt = createdAt.value,
            description = description?.value,
            licenses = licenses?.value,
            homepageUrl = homepageUrl?.value,
            repositoryUrl = repositoryUrl?.value,
            sizeInBytes = sizeInBytes?.value,
            checksums = checksums?.value
        )
    }
}