package dev.rubentxu.hodei.packages.application.audit

import dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId
import dev.rubentxu.hodei.packages.domain.model.attestation.Attestation

interface SupplyChainAuditService {
    suspend fun auditRelease(artifactId: ArtifactId): AuditReport
}

data class AuditReport(
    val attestations: List<Attestation>,
    val provenanceVerified: Boolean
)
