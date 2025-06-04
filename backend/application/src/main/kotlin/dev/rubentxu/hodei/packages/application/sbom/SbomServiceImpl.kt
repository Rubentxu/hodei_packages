package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.CreateSbomRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisResponse
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomDocumentResponse
import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.repository.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.repository.sbom.SbomRepository
import dev.rubentxu.hodei.packages.domain.service.sbom.SbomGenerator
import dev.rubentxu.hodei.packages.domain.service.sbom.SbomAnalyzerPort // Added import
import dev.rubentxu.hodei.packages.application.sbom.dto.VulnerabilityDto // Added import
import dev.rubentxu.hodei.packages.application.sbom.dto.LicenseComplianceDto // Added import
import java.time.Instant

/**
 * Implementación del servicio de aplicación para operaciones SBOM.
 * Este servicio orquesta la interacción entre la interfaz de usuario y el dominio,
 * utilizando DTOs para la transferencia de datos.
 */
class SbomServiceImpl(
    private val sbomRepository: SbomRepository,
    private val sbomGenerator: SbomGenerator,
    private val eventPublisher: EventPublisher,
    private val artifactStorage: ArtifactStoragePort,
    private val sbomAnalyzerPort: SbomAnalyzerPort, // Added SbomAnalyzerPort
) : SbomService {
    /**
     * Crea un nuevo documento SBOM a partir de la información proporcionada.
     */
    override suspend fun createSbom(request: CreateSbomRequest): Result<SbomDocumentResponse> {
        return try {
            val sbomDocument = request.toSbomDocument()

            val result = sbomRepository.save(sbomDocument)

            result.map { document ->
                SbomDocumentResponse.fromDomainDocument(document)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Genera un documento SBOM a partir del contenido de un artefacto.
     */
    override suspend fun generateSbomFromArtifact(
        artifactId: String,
        format: String,
    ): Result<SbomDocumentResponse> {
        return try {
            // Obtener el contenido del artefacto
            val contentResult = artifactStorage.retrieveArtifactContent(artifactId)

            if (contentResult.isFailure) {
                return Result.failure(
                    contentResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve artifact content"),
                )
            }

            val content = contentResult.getOrNull()
            if (content == null) {
                return Result.failure(IllegalStateException("No content available for artifact $artifactId"))
            }

            // Convertir el formato de string a enum
            val sbomFormatEnum = SbomFormat.fromString(format)
                ?: return Result.failure(IllegalArgumentException("Invalid or unsupported SBOM format string: $format"))

            // Generar el SBOM
            val generationResult = sbomGenerator.generateSbom(artifactId, content, sbomFormatEnum)

            generationResult.map { document ->
                SbomDocumentResponse.fromDomainDocument(document)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene un documento SBOM por su ID.
     */
    override suspend fun getSbomById(sbomId: String): Result<SbomDocumentResponse> {
        return try {
            val result = sbomRepository.findById(sbomId)

            result.map { document ->
                if (document != null) {
                    SbomDocumentResponse.fromDomainDocument(document)
                } else {
                    throw NoSuchElementException("SBOM document with ID $sbomId not found")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los documentos SBOM asociados a un artefacto.
     */
    override suspend fun getSbomsByArtifactId(artifactId: String): Result<List<SbomDocumentResponse>> {
        return try {
            val result = sbomRepository.findByArtifactId(artifactId)

            result.map { documents ->
                documents.map { SbomDocumentResponse.fromDomainDocument(it) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la versión más reciente del documento SBOM para un artefacto.
     */
    override suspend fun getLatestSbomByArtifactId(artifactId: String): Result<SbomDocumentResponse> {
        return try {
            val result = sbomRepository.findLatestByArtifactId(artifactId)

            result.map { document ->
                if (document != null) {
                    SbomDocumentResponse.fromDomainDocument(document)
                } else {
                    throw NoSuchElementException("Latest SBOM document for artifact ID $artifactId not found")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca documentos SBOM que contengan un componente específico.
     */
    override suspend fun findSbomsByComponent(
        componentName: String,
        componentVersion: String?,
    ): Result<List<SbomDocumentResponse>> {
        return try {
            val result = sbomRepository.findByComponent(componentName, componentVersion)

            result.map { documents ->
                documents.map { SbomDocumentResponse.fromDomainDocument(it) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Realiza un análisis de un documento SBOM (vulnerabilidades, licencias, etc).
     * Este es un método esqueleto que se implementará más adelante con un
     * servicio real de análisis de vulnerabilidades.
     */
    override suspend fun analyzeSbom(request: SbomAnalysisRequest): Result<SbomAnalysisResponse> {
        return try {
            // Obtener el documento SBOM
            val sbomResult = sbomRepository.findById(request.sbomId)

            if (sbomResult.isFailure) {
                return Result.failure(
                    sbomResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve SBOM document"),
                )
            }

            val sbomDocument = sbomResult.getOrNull()
            if (sbomDocument == null) {
                return Result.failure(IllegalArgumentException("SBOM document with ID ${request.sbomId} not found"))
            }

            // Realizar el análisis utilizando SbomAnalyzerPort
            val domainAnalysisResult = sbomAnalyzerPort.performAnalysis(sbomDocument, request.analysisTypes)

            if (domainAnalysisResult.isFailure) {
                return Result.failure(
                    domainAnalysisResult.exceptionOrNull() ?: IllegalStateException("Failed to perform SBOM analysis")
                )
            }

            val actualDomainAnalysis = domainAnalysisResult.getOrNull()
                ?: return Result.failure(IllegalStateException("SBOM analysis returned no result despite success"))

            // Mapear el resultado del dominio al DTO de respuesta
            val response = SbomAnalysisResponse(
                sbomId = request.sbomId,
                analysisTimestamp = actualDomainAnalysis.analysisTimestamp,
                vulnerabilities = actualDomainAnalysis.vulnerabilities.map { dv ->
                    VulnerabilityDto(
                        id = dv.id,
                        severity = dv.severity,
                        description = dv.description,
                        componentName = dv.componentName,
                        componentVersion = dv.componentVersion
                    )
                },
                licenseCompliance = actualDomainAnalysis.licenseCompliance?.let { dl ->
                    LicenseComplianceDto(
                        status = dl.status,
                        issues = dl.issues
                    )
                },
                analysisSummary = actualDomainAnalysis.summary
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
