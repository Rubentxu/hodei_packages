package dev.rubentxu.hodei.packages.domain.model.sbom

data class DomainLicenseInfo(
    val status: String, // e.g., "COMPLIANT", "NON_COMPLIANT", "UNKNOWN"
    val issues: List<String> // Descriptions of any license issues
)