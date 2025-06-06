package dev.rubentxu.hodei.packages.application.integrityverification.dto

/**
 * Data Transfer Object for requesting the generation of an SBOM from an artifact.
 */
data class GenerateSbomFromArtifactRequest(
    val artifactId: String,
    val format: String
)
