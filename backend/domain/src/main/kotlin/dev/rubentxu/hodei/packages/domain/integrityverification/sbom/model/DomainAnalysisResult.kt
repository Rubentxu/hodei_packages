package dev.rubentxu.hodei.packages.domain.model.sbom

import java.time.Instant
import java.util.UUID

/**
 * Represents the comprehensive result of an analysis performed on an SBOM document.
 *
 * @param id Unique identifier for this specific analysis result instance.
 * @param sbomDocumentId The ID of the SbomDocument that was analyzed.
 * @param analysisTimestamp Timestamp indicating when the analysis was completed.
 * @param requestedAnalysisTypes The list of analysis types that were initially requested (e.g., "VULNERABILITIES", "LICENSES").
 * @param performedAnalysisTypes The list of analysis types that were successfully performed.
 * @param vulnerabilities List of vulnerability details found. Empty if none found or if vulnerability analysis was not performed.
 * @param licenseCompliance License compliance information. Null if license analysis was not performed or is not applicable.
 * @param summary An optional overall summary of the analysis findings.
 * @param errors A list of errors encountered during the analysis process itself. Empty if no errors occurred.
 */
data class DomainAnalysisResult(
    val id: String = UUID.randomUUID().toString(),
    val sbomDocumentId: String,
    val analysisTimestamp: Instant,
    val requestedAnalysisTypes: List<String>,
    val performedAnalysisTypes: List<String>,
    val vulnerabilities: List<DomainVulnerabilityInfo> = emptyList(),
    val licenseCompliance: DomainLicenseInfo? = null,
    val summary: String? = null,
    val errors: List<AnalysisErrorLog> = emptyList()
)


/**
 * Represents an error that occurred during a specific part of the SBOM analysis process.
 * This is for logging issues with the analysis execution itself, not for findings like vulnerabilities.
 *
 * @param analysisType The type or stage of analysis that encountered the error (e.g., "VULNERABILITY_SCAN", "LICENSE_CHECK").
 * @param errorMessage A concise message describing the error.
 * @param errorDetails Optional additional details, such as a stack trace or context-specific information.
 */
data class AnalysisErrorLog(
    val analysisType: String,
    val errorMessage: String,
    val errorDetails: String? = null
)