package dev.rubentxu.hodei.packages.application.policy

import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import dev.rubentxu.hodei.packages.domain.model.policy.PolicyId
import dev.rubentxu.hodei.packages.domain.model.policy.SecurityPolicy

interface SecurityPolicyService {
    suspend fun enforcePolicies(artifact: Artifact): PolicyEnforcementResult
    suspend fun definePolicy(policy: SecurityPolicy): PolicyId
}

data class PolicyEnforcementResult(
    val allowed: Boolean,
    val violatedPolicies: List<PolicyId>
)
