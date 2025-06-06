package dev.rubentxu.hodei.packages.application.sbom


import dev.rubentxu.hodei.packages.application.sbom.dto.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.common.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomAnalysisCompletedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomStoredEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.DomainAnalysisResult
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomAnalyzerPort
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomRepository
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.service.SbomGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [SbomService] that provides SBOM (Software Bill of Materials) management capabilities.
 *
 * This service handles the following operations:
 * - Creating and storing SBOM documents manually or generating them from artifacts
 * - Retrieving SBOM documents by various criteria (ID, artifact ID, component)
 * - Analyzing SBOM documents for security vulnerabilities and license compliance
 *
 * The implementation interacts with the domain layer through repositories and ports.
 */
class SbomServiceImpl(
    private val sbomRepository: SbomRepository,
    private val sbomGenerator: SbomGenerator,
    private val eventPublisher: EventPublisher,
    private val artifactStorage: ArtifactStoragePort,
    private val sbomAnalyzerPort: SbomAnalyzerPort,
) : SbomService {

    /**
     * Creates a new SBOM document from the provided information.
     *
     * @param request Request with necessary information to create the SBOM document
     * @return Result with the created SBOM document or failure with SbomError
     */
    override suspend fun createSbom(request: CreateSbomRequest): Result<SbomDocumentResponse> {
        if (request.artifactId.isBlank()) {
            return Result.failure(SbomError.ValidationFailed("Artifact ID cannot be empty in CreateSbomRequest"))
        }

        return withContext(Dispatchers.IO) {
            try {
                // Convert request to domain model
                val domainSbomDocument = request.toSbomDocument()

                // Save document to repository
                val savedDocument = sbomRepository.save(domainSbomDocument).getOrElse {
                    return@withContext Result.failure(
                        SbomError.RepositoryError("Failed to save SBOM: ${it.message}", it)
                    )
                }

                // Publish event about stored SBOM
                eventPublisher.publish(SbomStoredEvent(sbomDocument = savedDocument))

                // Return success with response DTO
                Result.success(SbomDocumentResponse.fromDomainDocument(savedDocument))
            } catch (e: IllegalArgumentException) {
                Result.failure(SbomError.ValidationFailed("Invalid SBOM request data: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(SbomError.UnexpectedError("Error processing SBOM request: ${e.message}", e))
            }
        }
    }

    /**
     * Generates an SBOM document from an artifact's content.
     * This method analyzes the artifact content and automatically extracts
     * component and dependency information.
     *
     * @param artifactId ID of the artifact to generate SBOM for
     * @param format Format of SBOM (CYCLONE_DX, SPDX)
     * @return Result with the generated SBOM document or failure with SbomError
     */
    override suspend fun generateSbomFromArtifact(
        artifactId: String,
        format: String,
    ): Result<SbomDocumentResponse> = withContext(Dispatchers.IO) {
        // Validate inputs
        if (artifactId.isBlank()) {
            return@withContext Result.failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
        }

        // Parse and validate format
        val sbomFormatEnum = try {
            SbomFormat.fromString(format)
                ?: return@withContext Result.failure(SbomError.ValidationFailed("Invalid SBOM format string: $format"))
        } catch (e: IllegalArgumentException) {
            return@withContext Result.failure(SbomError.ValidationFailed("Invalid SBOM format string: $format"))
        }

        try {
            // Retrieve artifact content
            val artifactContent = artifactStorage.download(artifactId).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.UnexpectedError("Failed to download artifact $artifactId: ${error.message}", error)
                )
            }

            if (artifactContent.isEmpty()) {
                return@withContext Result.failure(
                    SbomError.ValidationFailed("Artifact $artifactId content is empty, cannot generate SBOM")
                )
            }

            // Generate SBOM using the available 'generate' method
            val generatedSbomDocument = sbomGenerator.generate(artifactContent, sbomFormatEnum).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.GenerationFailed(
                        "Failed to generate SBOM for artifact $artifactId: ${error.message}",
                        error
                    )
                )
            }

            // Since the generate method doesn't take an artifactId, we need to create a new document with the correct artifactId
            val sbomWithArtifactId = generatedSbomDocument.copy(artifactId = artifactId)

            // Save the generated SBOM
            val savedSbomDoc = sbomRepository.save(sbomWithArtifactId).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.RepositoryError("Failed to save generated SBOM: ${error.message}", error)
                )
            }

            // Publish event
            eventPublisher.publish(SbomStoredEvent(sbomDocument = savedSbomDoc))

            Result.success(SbomDocumentResponse.fromDomainDocument(savedSbomDoc))
        } catch (e: Exception) {
            Result.failure(
                SbomError.UnexpectedError(
                    "Unexpected error processing artifact $artifactId: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Retrieves an SBOM document by its ID.
     *
     * @param sbomId ID of the SBOM document to retrieve
     * @return Result with the requested SBOM document or failure with SbomError
     */
    override suspend fun getSbomById(sbomId: String): Result<SbomDocumentResponse> = withContext(Dispatchers.IO) {
        if (sbomId.isBlank()) {
            return@withContext Result.failure(SbomError.ValidationFailed("SBOM ID cannot be empty"))
        }

        try {
            val sbomDocument = sbomRepository.findById(sbomId).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.RepositoryError("Failed to retrieve SBOM $sbomId: ${error.message}", error)
                )
            } ?: return@withContext Result.failure(SbomError.SbomNotFound(sbomId))

            Result.success(SbomDocumentResponse.fromDomainDocument(sbomDocument))
        } catch (e: Exception) {
            Result.failure(SbomError.UnexpectedError("Unexpected error retrieving SBOM $sbomId: ${e.message}", e))
        }
    }

    /**
     * Gets all SBOM documents associated with an artifact.
     *
     * @param artifactId ID of the artifact
     * @return Result with list of SBOM documents associated with the artifact or failure with SbomError
     */
    override suspend fun getSbomsByArtifactId(artifactId: String): Result<List<SbomDocumentResponse>> =
        withContext(Dispatchers.IO) {
            if (artifactId.isBlank()) {
                return@withContext Result.failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
            }

            try {
                val sbomDocuments = sbomRepository.findByArtifactId(artifactId).getOrElse { error ->
                    return@withContext Result.failure(
                        SbomError.RepositoryError(
                            "Failed to retrieve SBOMs for artifact $artifactId: ${error.message}",
                            error
                        )
                    )
                }

                Result.success(sbomDocuments.map { SbomDocumentResponse.fromDomainDocument(it) })
            } catch (e: Exception) {
                Result.failure(
                    SbomError.UnexpectedError(
                        "Unexpected error retrieving SBOMs for artifact $artifactId: ${e.message}",
                        e
                    )
                )
            }
        }

    /**
     * Gets the most recent version of an SBOM document for an artifact.
     *
     * @param artifactId ID of the artifact
     * @param format Optional format of the SBOM to filter by
     * @return Result with the latest SBOM document of the artifact or failure with SbomError
     */
    override suspend fun getLatestSbomByArtifactId(
        artifactId: String,
        format: String?
    ): Result<SbomDocumentResponse> = withContext(Dispatchers.IO) {
        if (artifactId.isBlank()) {
            return@withContext Result.failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
        }

        // Parse format if provided
        val sbomFormatEnum: SbomFormat? = format?.let {
            try {
                SbomFormat.fromString(it)
                    ?: return@withContext Result.failure(SbomError.ValidationFailed("Invalid SBOM format string: $it"))
            } catch (e: IllegalArgumentException) {
                return@withContext Result.failure(SbomError.ValidationFailed("Invalid SBOM format string: $it"))
            }
        }

        try {
            val latestSbom = sbomRepository.findLatestByArtifactId(artifactId, sbomFormatEnum).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.RepositoryError("Failed to get latest SBOM for $artifactId: ${error.message}", error)
                )
            } ?: return@withContext Result.failure(
                SbomError.SbomNotFound("No SBOM found for artifact $artifactId with specified format")
            )

            Result.success(SbomDocumentResponse.fromDomainDocument(latestSbom))
        } catch (e: Exception) {
            Result.failure(
                SbomError.UnexpectedError("Unexpected error retrieving latest SBOM for $artifactId: ${e.message}", e)
            )
        }
    }

    /**
     * Searches for SBOM documents containing a specific component.
     *
     * @param componentName Name of the component to search for
     * @param componentVersion Optional version of the component
     * @return Result with list of SBOM documents containing the specified component or failure with SbomError
     */
    override suspend fun findSbomsByComponent(
        componentName: String,
        componentVersion: String?
    ): Result<List<SbomDocumentResponse>> = withContext(Dispatchers.IO) {
        if (componentName.isBlank()) {
            return@withContext Result.failure(SbomError.ValidationFailed("Component name cannot be empty"))
        }

        try {
            val sbomDocuments = sbomRepository.findByComponent(componentName, componentVersion).getOrElse { error ->
                return@withContext Result.failure(
                    SbomError.RepositoryError(
                        "Failed to find SBOMs by component $componentName: ${error.message}",
                        error
                    )
                )
            }

            Result.success(sbomDocuments.map { SbomDocumentResponse.fromDomainDocument(it) })
        } catch (e: Exception) {
            Result.failure(
                SbomError.UnexpectedError(
                    "Unexpected error finding SBOMs by component $componentName: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Performs an analysis of an SBOM document for vulnerabilities, license compliance, or other issues.
     *
     * @param request Details of the analysis to perform, including the SBOM ID and analysis types
     * @return Result with the analysis results or failure with SbomError
     */
    override suspend fun analyzeSbom(request: SbomAnalysisRequest): Result<SbomAnalysisResponse> =
        withContext(Dispatchers.IO) {
            if (request.sbomId.isBlank()) {
                return@withContext Result.failure(SbomError.ValidationFailed("SBOM ID for analysis cannot be empty"))
            }

            try {
                // Retrieve SBOM document
                val sbomDocument = sbomRepository.findById(request.sbomId).getOrElse { error ->
                    return@withContext Result.failure(
                        SbomError.RepositoryError(
                            "Failed to retrieve SBOM for analysis ${request.sbomId}: ${error.message}",
                            error
                        )
                    )
                } ?: return@withContext Result.failure(SbomError.SbomNotFound(request.sbomId))

                // Perform analysis through port
                val domainAnalysisResult =
                    sbomAnalyzerPort.performAnalysis(sbomDocument, request.analysisTypes).getOrElse { error ->
                        return@withContext Result.failure(
                            SbomError.AnalysisFailed("SBOM analysis failed: ${error.message}", error)
                        )
                    }

                // Construct analytics data and publish event
                val analysisTypeString = request.analysisTypes.joinToString(", ")
                val issuesCount = domainAnalysisResult.vulnerabilities.sumOf { it.size } +
                        (domainAnalysisResult.licenseCompliance?.issues?.size ?: 0)

                // Publish analysis completion event
                eventPublisher.publish(
                    SbomAnalysisCompletedEvent(
                        sbomId = request.sbomId,
                        artifactId = sbomDocument.artifactId,
                        analysisType = analysisTypeString,
                        issues = issuesCount,
                        summaryData = domainAnalysisResult.summary ?: emptyMap()
                    )
                )

                // Map domain result to response DTO
                Result.success(createAnalysisResponse(request.sbomId, domainAnalysisResult))
            } catch (e: Exception) {
                Result.failure(
                    SbomError.UnexpectedError(
                        "Unexpected error analyzing SBOM ${request.sbomId}: ${e.message}",
                        e
                    )
                )
            }
        }

    /**
     * Helper method to create an analysis response DTO from domain analysis result.
     */
    private fun createAnalysisResponse(sbomId: String, domainResult: DomainAnalysisResult): SbomAnalysisResponse {
        return SbomAnalysisResponse(
            sbomId = sbomId,
            analysisTimestamp = domainResult.analysisTimestamp,
            vulnerabilities = domainResult.vulnerabilities.flatMap { (componentName, vulnList) ->
                vulnList.map { vulnInfo ->
                    VulnerabilityDto(
                        id = vulnInfo.id,
                        severity = vulnInfo.severity?.name ?: "UNKNOWN",
                        description = vulnInfo.description,
                        componentName = componentName,
                        componentVersion = null
                    )
                }
            },
            licenseCompliance = domainResult.licenseCompliance?.let { licenseInfo ->
                LicenseComplianceDto(
                    status = licenseInfo.status,
                    issues = licenseInfo.issues
                )
            },
            analysisSummary = domainResult.summary
        )
    }
}