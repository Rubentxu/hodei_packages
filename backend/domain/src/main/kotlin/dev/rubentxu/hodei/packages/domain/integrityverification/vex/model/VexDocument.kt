package dev.rubentxu.hodei.packages.domain.integrityverification.vex.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomId
import java.time.Instant

data class VexDocument(
    val id: String,
    val artifactId: ArtifactId,
    val sbomId: SbomId? = null,
    val cveId: String,
    val status: String,
    val issuedAt: Instant = Instant.now(),
    val details: String? = null
)
