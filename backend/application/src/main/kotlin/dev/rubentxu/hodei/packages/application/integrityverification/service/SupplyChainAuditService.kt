package dev.rubentxu.hodei.packages.application.integrityverification.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.integrityverification.attestation.model.Attestation


interface SupplyChainAuditService {
    suspend fun auditRelease(artifactId: ArtifactId): AuditReport
}

data class AuditReport(
    val attestations: List<Attestation>,
    val provenanceVerified: Boolean
)
