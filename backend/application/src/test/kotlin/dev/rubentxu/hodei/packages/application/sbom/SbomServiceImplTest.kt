package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.CreateSbomRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomComponentDto
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomDocumentResponse
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomRelationshipDto
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisRequest
import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomRelationship
import dev.rubentxu.hodei.packages.domain.repository.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.repository.sbom.SbomRepository
import dev.rubentxu.hodei.packages.domain.service.sbom.SbomGenerator
import dev.rubentxu.hodei.packages.domain.service.sbom.SbomAnalyzerPort
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainLicenseInfo
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainVulnerabilityInfo
import dev.rubentxu.hodei.packages.domain.model.sbom.DomainAnalysisResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import java.time.Instant

class SbomServiceImplTest : FunSpec({

    // Mocks de los servicios y repositorios necesarios
    val mockSbomRepository = mockk<SbomRepository>()
    val mockEventPublisher = mockk<EventPublisher>(relaxed = true)
    val mockSbomGenerator = mockk<SbomGenerator>()
    val mockArtifactStorage = mockk<ArtifactStoragePort>()
    val mockSbomAnalyzerPort = mockk<SbomAnalyzerPort>() // Added mock for SbomAnalyzerPort

    // Servicio a probar
    val sbomService =
        SbomServiceImpl(
            sbomRepository = mockSbomRepository,
            sbomGenerator = mockSbomGenerator,
            eventPublisher = mockEventPublisher,
            artifactStorage = mockArtifactStorage,
            sbomAnalyzerPort = mockSbomAnalyzerPort, // Added SbomAnalyzerPort to constructor
        )

    beforeEach {
        clearAllMocks()
    }

    // Datos de prueba comunes
    val artifactId = "test-artifact-123"
    val sbomId = "test-sbom-456"
    val now = Instant.now()

    val testComponent =
        SbomComponent(
            name = "testLib",
            version = "1.0.0",
            type = "library",

        )

    val testRelationship =
        SbomRelationship(
            fromComponentId = "comp1",
            toComponentId = "comp2",
            type = "DEPENDS_ON",
        )

    val testDocument =
        SbomDocument(

            artifactId = artifactId,
            format = SbomFormat.CYCLONE_DX,
            version = "1.0.0",
            creationTime = now,
            components = listOf(testComponent),
            relationships = listOf(testRelationship),
        )

    test("createSbom should store a new SBOM document") {
        // Preparar
        val request =
            CreateSbomRequest(
                artifactId = artifactId,
                format = "CYCLONE_DX",
                version = "1.0.0",
                components =
                    listOf(
                        SbomComponentDto(
                            version = "1.0.0",
                            type = "library",
                            name = "test-component-name"
                        ),

                    ),
                relationships =
                    listOf(
                        SbomRelationshipDto(
                            fromComponentId = "comp1",
                            toComponentId = "comp2",
                            type = "DEPENDS_ON",
                        ),
                    ),
            )

        val capturedDocument = slot<SbomDocument>()

        coEvery {
            mockSbomRepository.save(capture(capturedDocument))
        } returns Result.success(testDocument)

        // Ejecutar
        val result = sbomService.createSbom(request)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.shouldBeInstanceOf<SbomDocumentResponse>()
        response.artifactId shouldBe artifactId
        response.format shouldBe "CYCLONE_DX"
        response.components.size shouldBe 1
        response.relationships.size shouldBe 1

        coVerify(exactly = 1) { mockSbomRepository.save(any()) }

        // Verificarque el documento pasado al repositorio tiene los valores correctos
        capturedDocument.captured.artifactId shouldBe artifactId
        capturedDocument.captured.format shouldBe SbomFormat.CYCLONE_DX
        capturedDocument.captured.version shouldBe "1.0.0"
        capturedDocument.captured.components.size shouldBe 1
        capturedDocument.captured.relationships.size shouldBe 1
    }

    test("generateSbomFromArtifact should extract SBOM data from artifact content") {
        // Preparar
        val format = "CYCLONE_DX"
        val artifactContent = "test content".toByteArray()

        coEvery {
            mockArtifactStorage.retrieveArtifactContent(artifactId)
        } returns Result.success(artifactContent)

        coEvery {
            mockSbomGenerator.generateSbom(
                artifactId,
                artifactContent,
                SbomFormat.CYCLONE_DX,
            )
        } returns Result.success(testDocument)

        // Ejecutar
        val result = sbomService.generateSbomFromArtifact(artifactId, format)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.artifactId shouldBe artifactId

        coVerify(exactly = 1) { mockArtifactStorage.retrieveArtifactContent(artifactId) }
        coVerify(exactly = 1) {
            mockSbomGenerator.generateSbom(
                artifactId,
                artifactContent,
                SbomFormat.CYCLONE_DX,
            )
        }
    }

    test("getSbomById should return SBOM document by id") {
        // Preparar
        coEvery {
            mockSbomRepository.findById(sbomId)
        } returns Result.success(testDocument)

        // Ejecutar
        val result = sbomService.getSbomById(sbomId)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.id shouldBe testDocument.id
        response.artifactId shouldBe artifactId

        coVerify(exactly = 1) { mockSbomRepository.findById(sbomId) }
    }

    test("getSbomsByArtifactId should return all SBOM documents for an artifact") {
        // Preparar
        val document1 = SbomDocument(
            artifactId = artifactId,
            format = SbomFormat.CYCLONE_DX,
            components = listOf(testComponent),
            relationships = listOf(testRelationship),
            version = "1.0.0",
            creationTime = now
        )

        val document2 = SbomDocument(
            artifactId = artifactId,
            format = SbomFormat.CYCLONE_DX,
            components = listOf(testComponent),
            relationships = listOf(testRelationship),
            version = "1.1.0",
            creationTime = now.plusSeconds(1) // Use a different time for unique ID
        )

        coEvery {
            mockSbomRepository.findByArtifactId(artifactId)
        } returns Result.success(listOf(document1, document2))

        // Ejecutar
        val result = sbomService.getSbomsByArtifactId(artifactId)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.size shouldBe 2

        coVerify(exactly = 1) { mockSbomRepository.findByArtifactId(artifactId) }
    }

    test("getLatestSbomByArtifactId should return the most recent SBOM") {
        // Preparar
        coEvery {
            mockSbomRepository.findLatestByArtifactId(artifactId)
        } returns Result.success(testDocument)

        // Ejecutar
        val result = sbomService.getLatestSbomByArtifactId(artifactId)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.artifactId shouldBe artifactId

        coVerify(exactly = 1) { mockSbomRepository.findLatestByArtifactId(artifactId) }
    }

    test("findSbomsByComponent should return SBOMs containing specific component") {
        // Preparar
        val componentName = "testLib"
        val componentVersion = "1.0.0"

        coEvery {
            mockSbomRepository.findByComponent(componentName, componentVersion)
        } returns Result.success(listOf(testDocument))

        // Ejecutar
        val result = sbomService.findSbomsByComponent(componentName, componentVersion)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.size shouldBe 1
        response[0].components.any { it.name == componentName } shouldBe true

        coVerify(exactly = 1) { mockSbomRepository.findByComponent(componentName, componentVersion) }
    }

    test("handleErrors should return failure results") {
        // Preparar
        val errorMessage = "Not found"
        coEvery {
            mockSbomRepository.findById("non-existent")
        } returns Result.failure(Exception(errorMessage))

        // Ejecutar
        val result = sbomService.getSbomById("non-existent")

        // Verificar
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }

    test("analyzeSbom should perform analysis and return mapped results") {
        // Preparar
        val sbomDocumentSlot = slot<SbomDocument>()
        val analysisTypesSlot = slot<List<String>>()
        val analysisTypes = listOf("VULNERABILITIES", "LICENSES")
        val request = SbomAnalysisRequest(sbomId = sbomId, analysisTypes = analysisTypes)
        val analysisTimestamp = Instant.now().minusSeconds(3600) // Un timestamp específico para la prueba

        val mockDomainVulnerability = DomainVulnerabilityInfo(
            id = "CVE-2023-001",
            severity = "HIGH",
            description = "Test vulnerability",
            componentName = testComponent.name,
            componentVersion = testComponent.version
        )
        val mockDomainLicenseInfo = DomainLicenseInfo(
            status = "COMPLIANT",
            issues = emptyList()
        )
        val mockDomainAnalysisResult = DomainAnalysisResult(
            vulnerabilities = listOf(mockDomainVulnerability),
            licenseCompliance = mockDomainLicenseInfo,
            summary = "Analysis completed successfully.",
            analysisTimestamp = analysisTimestamp
        )

        coEvery { mockSbomRepository.findById(sbomId) } returns Result.success(testDocument)
        coEvery { mockSbomAnalyzerPort.performAnalysis(any(), any()) } returns Result.success(mockDomainAnalysisResult)

        // Ejecutar
        val result = sbomService.analyzeSbom(request)

        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.sbomId shouldBe sbomId
        response.analysisTimestamp shouldBe analysisTimestamp
        response.vulnerabilities.size shouldBe 1
        response.vulnerabilities[0].id shouldBe mockDomainVulnerability.id
        response.vulnerabilities[0].severity shouldBe mockDomainVulnerability.severity
        response.licenseCompliance?.status shouldBe mockDomainLicenseInfo.status
        response.analysisSummary shouldBe mockDomainAnalysisResult.summary

        coVerify(exactly = 1) { mockSbomRepository.findById(sbomId) }
        coVerify(atLeast = 1) { mockSbomAnalyzerPort.performAnalysis(capture(sbomDocumentSlot), capture(analysisTypesSlot)) }
    }
})
