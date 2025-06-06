package dev.rubentxu.hodei.packages.application.sbom

sealed class SbomError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationFailed(reason: String) : SbomError("Validation failed: $reason")
    class SbomNotFound(sbomId: String) : SbomError("SBOM with ID '$sbomId' not found.")
    class ArtifactNotFound(artifactId: String) : SbomError("Artifact with ID '$artifactId' not found.")
    class AnalysisFailed(message: String, cause: Throwable? = null) : SbomError(message, cause)
    class GenerationFailed(message: String, cause: Throwable? = null) : SbomError(message, cause)
    class RepositoryError(message: String, cause: Throwable? = null) : SbomError(message, cause)
    class UnexpectedError(message: String, cause: Throwable? = null) : SbomError(message, cause)
}
