package dev.rubentxu.hodei.packages.domain.policymanagement.model

import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.time.Instant

@JvmInline
value class PolicyId(val value: String)

enum class PolicyType { BLOCK_CVE, REQUIRE_SIGNATURE, CUSTOM }
enum class PolicyEffect { ALLOW, DENY }

data class SecurityPolicy(
    val id: PolicyId,
    val type: PolicyType,
    val effect: PolicyEffect,
    val criteria: Map<String, String>, // Ej: {"cve": "CVE-2025-1234"}
    val description: String,
    val createdBy: UserId,
    val createdAt: Instant,
    val enabled: Boolean = true
)
