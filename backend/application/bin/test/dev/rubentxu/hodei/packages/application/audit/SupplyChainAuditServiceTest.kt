package dev.rubentxu.hodei.packages.application.audit

import dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId
import dev.rubentxu.hodei.packages.domain.model.attestation.Attestation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SupplyChainAuditServiceTest : BehaviorSpec({
    given("un artifactId válido para auditoría") {
        val artifactId = ArtifactId("artifact-1")
        val fakeService = object : SupplyChainAuditService {
            override suspend fun auditRelease(artifactId: ArtifactId): AuditReport {
                return AuditReport(
                    attestations = listOf(Attestation("attestation-1", "provenance")),
                    provenanceVerified = true
                )
            }
        }

        `when`("se solicita la auditoría de la release") {
            val result = fakeService.auditRelease(artifactId)
            then("el resultado debe contener atestaciones y procedencia verificada") {
                result.attestations.size shouldBe 1
                result.attestations[0].id shouldBe "attestation-1"
                result.provenanceVerified shouldBe true
            }
        }
    }
})
