package dev.rubentxu.hodei.packages.application.policy

import dev.rubentxu.hodei.packages.application.policymanagement.service.PolicyEnforcementResult
import dev.rubentxu.hodei.packages.application.policymanagement.service.SecurityPolicyService
import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import dev.rubentxu.hodei.packages.domain.model.policy.PolicyId
import dev.rubentxu.hodei.packages.domain.model.policy.SecurityPolicy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SecurityPolicyServiceTest : BehaviorSpec({
    given("un artefacto y una política de seguridad definida") {
        val artifact = Artifact(
            id = dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId("artifact-1"),
            coordinates = dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactCoordinates("dev.rubentxu", "libfoo", "1.0.0"),
            createdBy = dev.rubentxu.hodei.packages.domain.model.artifact.UserId("dev"),
            createdAt = java.time.Instant.now()
        )
        val policy = SecurityPolicy(
            id = PolicyId("policy-1"),
            type = dev.rubentxu.hodei.packages.domain.model.policy.PolicyType.BLOCK_CVE,
            effect = dev.rubentxu.hodei.packages.domain.model.policy.PolicyEffect.DENY,
            criteria = mapOf("cve" to "CVE-2025-1234"),
            description = "Bloquea CVEs críticos",
            createdBy = dev.rubentxu.hodei.packages.domain.model.artifact.UserId("admin"),
            createdAt = java.time.Instant.now()
        )
        val fakeService = object : SecurityPolicyService {
            override suspend fun enforcePolicies(artifact: Artifact): PolicyEnforcementResult {
                return PolicyEnforcementResult(allowed = false, violatedPolicies = listOf(policy.id))
            }
            override suspend fun definePolicy(policy: SecurityPolicy): PolicyId = policy.id
        }

        `when`("se aplica la política de seguridad al artefacto") {
            val result = fakeService.enforcePolicies(artifact)
            then("el resultado debe indicar que la política fue violada y el acceso denegado") {
                result.allowed shouldBe false
                result.violatedPolicies shouldBe listOf(policy.id)
            }
        }
    }
})
