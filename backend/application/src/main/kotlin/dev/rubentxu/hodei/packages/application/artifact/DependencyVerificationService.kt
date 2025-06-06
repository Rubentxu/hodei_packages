package dev.rubentxu.hodei.packages.application.artifact

import dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomId
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainVulnerabilityInfo

interface DependencyVerificationService {
    suspend fun verify(command: VerifyDependencyCommand): VerificationResult
}

data class VerifyDependencyCommand(
    val artifactId: ArtifactId,
    val sbomId: SbomId
)

data class VerificationResult(
    val isSignatureValid: Boolean,
    val isMerkleValid: Boolean,
    val vulnerabilities: List<DomainVulnerabilityInfo>
)
