package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.CreateSbomRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisResponse
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomDocumentResponse


/**
 * Application service interface for SBOM operations.
 * Defines available operations for SBOM document management.
 */
interface SbomService {
    /**
     * Creates a new SBOM document from the provided information.
     *
     * @param request Request with necessary information to create the SBOM document
     * @return Result with the created SBOM document or failure with SbomError
     */
    suspend fun createSbom(request: CreateSbomRequest): Result<SbomDocumentResponse>

    /**
     * Generates an SBOM document from an artifact's content.
     * This method analyzes the artifact content and automatically extracts
     * component and dependency information.
     *
     * @param artifactId ID of the artifact to generate SBOM for
     * @param format Format of SBOM (CYCLONE_DX, SPDX)
     * @return Result with the generated SBOM document or failure with SbomError
     */
    suspend fun generateSbomFromArtifact(
        artifactId: String,
        format: String,
    ): Result<SbomDocumentResponse>

    /**
     * Retrieves an SBOM document by its ID.
     *
     * @param sbomId ID of the SBOM document to retrieve
     * @return Result with the requested SBOM document or failure with SbomError
     */
    suspend fun getSbomById(sbomId: String): Result<SbomDocumentResponse>

    /**
     * Gets all SBOM documents associated with an artifact.
     *
     * @param artifactId ID of the artifact
     * @return Result with list of SBOM documents associated with the artifact or failure with SbomError
     */
    suspend fun getSbomsByArtifactId(artifactId: String): Result<List<SbomDocumentResponse>>

    /**
     * Gets the most recent version of an SBOM document for an artifact.
     *
     * @param artifactId ID of the artifact
     * @param format Optional format of the SBOM to filter by
     * @return Result with the latest SBOM document of the artifact or failure with SbomError
     */
    suspend fun getLatestSbomByArtifactId(artifactId: String, format: String? = null): Result<SbomDocumentResponse>

    /**
     * Searches for SBOM documents containing a specific component.
     *
     * @param componentName Name of the component to search for
     * @param componentVersion Optional version of the component
     * @return Result with list of SBOM documents containing the specified component or failure with SbomError
     */
    suspend fun findSbomsByComponent(
        componentName: String,
        componentVersion: String? = null,
    ): Result<List<SbomDocumentResponse>>

    /**
     * Performs an analysis of an SBOM document (vulnerabilities, licenses, etc).
     *
     * @param request Details of the analysis to perform
     * @return Result with the analysis results or failure with SbomError
     */
    suspend fun analyzeSbom(request: SbomAnalysisRequest): Result<SbomAnalysisResponse>
}
