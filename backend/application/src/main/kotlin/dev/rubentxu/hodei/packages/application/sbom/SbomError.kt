package dev.rubentxu.hodei.packages.application.sbom

sealed class SbomError(open val message: String) {
    data class ValidationFailed(val reason: String) : SbomError("Validation failed: $reason")
    data class SbomNotFound(val sbomId: String) : SbomError("SBOM with ID '$sbomId' not found.")
    data class ArtifactNotFound(val artifactId: String) : SbomError("Artifact with ID '$artifactId' not found.")
    data class AnalysisFailed(override val message: String, val cause: Throwable? = null) : SbomError(message)
    data class GenerationFailed(override val message: String, val cause: Throwable? = null) : SbomError(message)
    data class RepositoryError(override val message: String, val cause: Throwable? = null) : SbomError(message)
    data class UnexpectedError(override val message: String, val cause: Throwable? = null) : SbomError(message)
}
