package dev.rubentxu.hodei.packages.domain.policymanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyEffect
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import java.time.Instant

data class PolicyEnforcedEvent(
    val policyId: PolicyId,
    val artifactId: ArtifactId,
    val effect: PolicyEffect,
    val enforcedAt: Instant
)