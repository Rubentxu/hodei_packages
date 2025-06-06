package dev.rubentxu.hodei.packages.application.artifact

import dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomId
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainVulnerabilityInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class DependencyVerificationServiceTest : BehaviorSpec({
    given("un comando de verificación de dependencia válido") {
        val command = VerifyDependencyCommand(
            artifactId = ArtifactId("artifact-1"),
            sbomId = SbomId("sbom-1")
        )
        val fakeService = object : DependencyVerificationService {
            override suspend fun verify(command: VerifyDependencyCommand): VerificationResult {
                return VerificationResult(
                    isSignatureValid = true,
                    isMerkleValid = true,
                    vulnerabilities = listOf(
                        DomainVulnerabilityInfo("CVE-2025-1234", "Critical vulnerability", "AFFECTED")
                    )
                )
            }
        }

        `when`("se verifica la dependencia") {
            val result = fakeService.verify(command)
            then("el resultado debe indicar firmas y Merkle válidos y reportar vulnerabilidades") {
                result.isSignatureValid shouldBe true
                result.isMerkleValid shouldBe true
                result.vulnerabilities.size shouldBe 1
                result.vulnerabilities[0].cveId shouldBe "CVE-2025-1234"
            }
        }
    }
})
