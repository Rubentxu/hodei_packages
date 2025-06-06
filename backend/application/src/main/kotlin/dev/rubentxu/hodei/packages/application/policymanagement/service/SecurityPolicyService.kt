package dev.rubentxu.hodei.packages.application.policymanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.SecurityPolicy


interface SecurityPolicyService {
    suspend fun enforcePolicies(artifact: Artifact): PolicyEnforcementResult
    suspend fun definePolicy(policy: SecurityPolicy): PolicyId
}

data class PolicyEnforcementResult(
    val allowed: Boolean,
    val violatedPolicies: List<PolicyId>
)
