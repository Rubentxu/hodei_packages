package dev.rubentxu.hodei.packages.domain.model.sbom

import java.time.Instant

data class DomainAnalysisResult(
    val vulnerabilities: List<DomainVulnerabilityInfo>,
    val licenseCompliance: DomainLicenseInfo?, // Nullable if not requested
    val summary: String?,
    val analysisTimestamp: Instant // Added timestamp of the analysis
)