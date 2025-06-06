package dev.rubentxu.hodei.packages.application.integrityverification.dto

import java.time.Instant

data class VulnerabilityDto(
    val id: String, // e.g., CVE-2023-12345
    val severity: String, // e.g., "HIGH", "MEDIUM", "LOW"
    val description: String,
    val componentName: String,
    val componentVersion: String?
)

data class LicenseComplianceDto(
    val status: String, // e.g., "COMPLIANT", "NON_COMPLIANT", "UNKNOWN"
    val issues: List<String> // Descriptions of any license issues
)

data class SbomAnalysisResponse(
    val sbomId: String,
    val analysisTimestamp: Instant,
    val vulnerabilities: List<VulnerabilityDto>,
    val licenseCompliance: LicenseComplianceDto?, // Nullable if not requested
    val analysisSummary: String?
)
