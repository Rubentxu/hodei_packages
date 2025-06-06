package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.*
import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainAnalysisResult
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomStoredEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomAnalysisCompletedEvent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.ports.artifacts.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomRepository
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomAnalyzerPort
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.service.SbomGenerator


class SbomServiceImpl(
    private val sbomRepository: SbomRepository,
    private val sbomGenerator: SbomGenerator,
    private val eventPublisher: EventPublisher,
    private val artifactStorage: ArtifactStoragePort,
    private val sbomAnalyzerPort: SbomAnalyzerPort,
) : SbomService {

    override suspend fun createSbom(request: CreateSbomRequest): Result<SbomDocumentResponse, SbomError> {
        if (request.artifactId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("Artifact ID cannot be empty in CreateSbomRequest"))
        }

        val domainSbomDocument = try {
            request.toSbomDocument()
        } catch (e: IllegalArgumentException) {
            return Result.Failure(SbomError.ValidationFailed("Invalid SBOM request data: ${e.message}"))
        } catch (e: Exception) {
            return Result.Failure(SbomError.UnexpectedError("Error processing SBOM request: ${e.message}", e))
        }

        return sbomRepository.save(domainSbomDocument).toSharedResult(
            successMap = { savedDocument: SbomDocument ->
                eventPublisher.publish(SbomStoredEvent(sbomDocument = savedDocument))
                SbomDocumentResponse.Companion.fromDomainDocument(savedDocument)
            },
            errorMap = { SbomError.RepositoryError("Failed to save SBOM: ${it.message}", it) }
        )
    }

    override suspend fun generateSbomFromArtifact(
        artifactId: String,
        format: String,
    ): Result<SbomDocumentResponse, SbomError> {
        if (artifactId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
        }
        val sbomFormatEnum = try {
            SbomFormat.fromString(format)
                ?: return Result.Failure(SbomError.ValidationFailed("Invalid SBOM format string: $format"))
        } catch (e: IllegalArgumentException) {
            return Result.Failure(SbomError.ValidationFailed("Invalid SBOM format string: $format"))
        }

        val artifactContentResult = artifactStorage.retrieveArtifactContent(artifactId)
            .toSharedResultOrNotFound(
                notFoundError = SbomError.ArtifactNotFound("Artifact $artifactId not found (content was null in Result)"),
                successMap = { it },
                errorMap = {
                    SbomError.UnexpectedError(
                        "Port failed to retrieve artifact $artifactId: ${it.message}",
                        it
                    )
                }
            )

        val artifactContent = when (artifactContentResult) {
            is Result.Success -> artifactContentResult.value
            is Result.Failure -> return artifactContentResult
        }

        if (artifactContent.isEmpty()) {
            return Result.Failure(SbomError.ValidationFailed("Artifact $artifactId content is empty, cannot generate SBOM"))
        }

        val generatedSbomDocumentResult = sbomGenerator.generateSbom(artifactId, artifactContent, sbomFormatEnum)
            .toSharedResult(
                successMap = { it },
                errorMap = {
                    SbomError.GenerationFailed(
                        "Port failed to generate SBOM for artifact $artifactId: ${it.message}",
                        it
                    )
                }
            )

        val generatedSbomDocument = when (generatedSbomDocumentResult) {
            is Result.Success -> generatedSbomDocumentResult.value
            is Result.Failure -> return generatedSbomDocumentResult
        }

        return sbomRepository.save(generatedSbomDocument).toSharedResult(
            successMap = { savedSbomDoc: SbomDocument ->
                eventPublisher.publish(SbomStoredEvent(sbomDocument = savedSbomDoc))
                SbomDocumentResponse.Companion.fromDomainDocument(savedSbomDoc)
            },
            errorMap = { SbomError.RepositoryError("Failed to save generated SBOM: ${it.message}", it) }
        )
    }

    override suspend fun getSbomById(sbomId: String): Result<SbomDocumentResponse, SbomError> {
        if (sbomId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("SBOM ID cannot be empty"))
        }
        return sbomRepository.findById(sbomId).toSharedResultOrNotFound(
            notFoundError = SbomError.SbomNotFound(sbomId),
            successMap = { SbomDocumentResponse.Companion.fromDomainDocument(it) },
            errorMap = { SbomError.RepositoryError("Failed to retrieve SBOM $sbomId: ${it.message}", it) }
        )
    }

    override suspend fun getSbomsByArtifactId(artifactId: String): Result<List<SbomDocumentResponse>, SbomError> {
        if (artifactId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
        }
        return sbomRepository.findByArtifactId(artifactId).toSharedResult(
            successMap = { docs -> docs.map { SbomDocumentResponse.Companion.fromDomainDocument(it) } },
            errorMap = {
                SbomError.RepositoryError(
                    "Failed to retrieve SBOMs for artifact $artifactId: ${it.message}",
                    it
                )
            }
        )
    }

    override suspend fun getLatestSbomByArtifactId(
        artifactId: String,
        format: String?
    ): Result<SbomDocumentResponse, SbomError> {
        if (artifactId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("Artifact ID cannot be empty"))
        }

        val sbomFormatEnum: SbomFormat? = format?.let {
            try {
                SbomFormat.fromString(it)
                    ?: return Result.Failure(SbomError.ValidationFailed("Invalid SBOM format string: $it"))
            } catch (e: IllegalArgumentException) {
                return Result.Failure(SbomError.ValidationFailed("Invalid SBOM format string: $it"))
            }
        }

        return sbomRepository.findLatestByArtifactId(artifactId, sbomFormatEnum).toSharedResultOrNotFound(
            notFoundError = SbomError.SbomNotFound("No SBOM found for artifact $artifactId with specified format"),
            successMap = { SbomDocumentResponse.Companion.fromDomainDocument(it) },
            errorMap = { SbomError.RepositoryError("Failed to get latest SBOM for $artifactId: ${it.message}", it) }
        )
    }

    override suspend fun findSbomsByComponent(
        componentName: String,
        componentVersion: String?
    ): Result<List<SbomDocumentResponse>, SbomError> {
        if (componentName.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("Component name cannot be empty"))
        }
        return sbomRepository.findByComponent(componentName, componentVersion).toSharedResult(
            successMap = { docs -> docs.map { SbomDocumentResponse.Companion.fromDomainDocument(it) } },
            errorMap = {
                SbomError.RepositoryError(
                    "Failed to find SBOMs by component $componentName: ${it.message}",
                    it
                )
            }
        )
    }

    override suspend fun analyzeSbom(request: SbomAnalysisRequest): Result<SbomAnalysisResponse, SbomError> {
        if (request.sbomId.isBlank()) {
            return Result.Failure(SbomError.ValidationFailed("SBOM ID for analysis cannot be empty"))
        }

        val sbomDocumentResult = sbomRepository.findById(request.sbomId).toSharedResultOrNotFound(
            notFoundError = SbomError.SbomNotFound(request.sbomId),
            successMap = { it },
            errorMap = {
                SbomError.RepositoryError(
                    "Failed to retrieve SBOM for analysis ${request.sbomId}: ${it.message}",
                    it
                )
            }
        )

        val sbomDocument : SbomDocument = when (sbomDocumentResult) {
            is Result.Success -> sbomDocumentResult.value
            is Result.Failure -> return Result.Failure(sbomDocumentResult.error)
        }

        val domainAnalysisResultKotlin = sbomAnalyzerPort.performAnalysis(sbomDocument, request.analysisTypes)

        return domainAnalysisResultKotlin.toSharedResult(
            successMap = { domainResult: DomainAnalysisResult ->
                val analysisTypeString = request.analysisTypes.joinToString(", ")
                val issuesCount = domainResult.vulnerabilities.values.sumOf { it.size } +
                        (domainResult.licenseCompliance?.issues?.size ?: 0)

                eventPublisher.publish(
                    SbomAnalysisCompletedEvent(
                        sbomId = request.sbomId,
                        artifactId = sbomDocument.artifactId,
                        analysisType = analysisTypeString,
                        issues = issuesCount,
                        summaryData = domainResult.summary ?: emptyMap()
                    )
                )

                SbomAnalysisResponse(
                    sbomId = request.sbomId,
                    analysisTimestamp = domainResult.analysisTimestamp,
                    vulnerabilities = domainResult.vulnerabilities.map { entry ->
                        entry.value.map { vulnerabilityInfo ->
                            VulnerabilityDto(
                                id = vulnerabilityInfo.cveId,
                                severity = vulnerabilityInfo.status,
                                description = vulnerabilityInfo.description,
                                componentName = entry.key,
                                componentVersion = null
                            )
                        }
                    }.flatten(),
                    licenseCompliance = domainResult.licenseCompliance?.let { dl ->
                        LicenseComplianceDto(
                            status = dl.status,
                            issues = dl.issues
                        )
                    },
                    analysisSummary = domainResult.summary
                )
            },
            errorMap = { SbomError.AnalysisFailed("SBOM analysis failed: ${it.message}", it) }
        )
    }
}
