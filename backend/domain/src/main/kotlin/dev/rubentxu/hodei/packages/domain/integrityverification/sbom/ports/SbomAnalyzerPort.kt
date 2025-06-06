package dev.rubentxu.hodei.packages.domain.ports.sbom

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainAnalysisResult


/**
 * Port for performing analysis on SBOM documents.
 */
interface SbomAnalyzerPort {
    /**
     * Performs analysis on the given SBOM document.
     *
     * @param document The SBOM document to analyze.
     * @param analysisTypes A list of types of analysis to perform (e.g., "VULNERABILITIES", "LICENSES").
     * @return A Result containing the [dev.rubentxu.hodei.packages.domain.model.sbom.DomainAnalysisResult] or an error.
     */
    suspend fun performAnalysis(
        document: SbomDocument,
        analysisTypes: List<String>
    ): Result<DomainAnalysisResult>
}

