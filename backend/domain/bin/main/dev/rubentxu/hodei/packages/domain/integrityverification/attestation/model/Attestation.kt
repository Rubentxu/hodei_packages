package dev.rubentxu.hodei.packages.domain.integrityverification.attestation.model

data class Attestation(
    val id: String,
    val type: String,
    val issuedAt: java.time.Instant? = null,
    val payload: String? = null
)
