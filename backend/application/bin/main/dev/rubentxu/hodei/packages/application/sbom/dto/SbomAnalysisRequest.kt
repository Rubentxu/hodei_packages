package dev.rubentxu.hodei.packages.application.sbom.dto

data class SbomAnalysisRequest(
    val sbomId: String,
    val analysisTypes: List<String> // e.g., "VULNERABILITIES", "LICENSES"
)
