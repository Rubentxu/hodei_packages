package dev.rubentxu.hodei.packages.application.artifactmanagement.service


import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.DomainVulnerabilityInfo
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomId


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
