package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.*
import dev.rubentxu.hodei.packages.application.shared.Result as SharedAppResult
import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.model.sbom.*
import dev.rubentxu.hodei.packages.domain.ports.artifacts.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomRepository
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomAnalyzerPort
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.service.SbomGenerator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import java.time.Instant

class SbomServiceImplTest : FunSpec({

    val mockSbomRepository = mockk<SbomRepository>()
    val mockEventPublisher = mockk<EventPublisher>(relaxed = true)
    val mockSbomGenerator = mockk<SbomGenerator>()
    val mockArtifactStorage = mockk<ArtifactStoragePort>()
    val mockSbomAnalyzerPort = mockk<SbomAnalyzerPort>()

    val sbomService = SbomServiceImpl(
        sbomRepository = mockSbomRepository,
        sbomGenerator = mockSbomGenerator,
        artifactStorage = mockArtifactStorage,
        eventPublisher = mockEventPublisher,
        sbomAnalyzerPort = mockSbomAnalyzerPort
    )

    val testComponentDto = SbomComponentDto(
        name = "Test Component",
        version = "1.0.0",
        type = "LIBRARY",
        supplier = "Test Supplier",
        purl = "pkg:generic/test-component@1.0.0",
        hashes = mapOf("SHA-256" to "test-hash"),
        licenses = listOf("Apache-2.0")
    )

    val testRelationshipDto = SbomRelationshipDto(
        fromComponentId = "comp1",
        toComponentId = "comp2",
        type = "DEPENDS_ON"
    )

    val testSbomDocument = SbomDocument(
        id = "test-sbom-id",
        artifactId = "test-artifact-id",
        format = SbomFormat.CYCLONE_DX,
        version = "1.4",
        creationTime = Instant.now(),
        components = listOf(SbomComponent(
            name = "Test Component", version = "1.0.0", type = "LIBRARY",
            supplier = "Test Supplier", purl = "pkg:generic/test-component@1.0.0",
            hashes = mapOf("SHA-256" to "test-hash"), licenses = listOf("Apache-2.0"))
        ),
        relationships = listOf(SbomRelationship(fromComponentId = "comp1", toComponentId = "comp2", type = "DEPENDS_ON"))
    )

    beforeEach {
        clearAllMocks()
        coEvery { mockEventPublisher.publish(any()) } just Runs
    }

    // --- createSbom Tests ---
    test("createSbom - exito") {
        val request = CreateSbomRequest(
            artifactId = "artifact1",
            format = "CycloneDX",
            version = "1.4",
            components = listOf(testComponentDto),
            relationships = listOf(testRelationshipDto)
        )
        coEvery { mockSbomRepository.save(any()) } coAnswers { kotlin.Result.success(arg(0) as SbomDocument) }

        val result = sbomService.createSbom(request)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<SbomDocumentResponse>
        successResult.value.artifactId shouldBe request.artifactId
        successResult.value.components.shouldHaveSize(1)
        coVerify(exactly = 1) { mockSbomRepository.save(any()) }
        coVerify(exactly = 1) { mockEventPublisher.publish(any(named("SbomCreatedEvent"))) }
    }

    test("createSbom - fallo por ID de artefacto vacio") {
        val request = CreateSbomRequest(
            artifactId = "", format = "CycloneDX", version = "1.4",
            components = emptyList(), relationships = emptyList()
        )

        val result = sbomService.createSbom(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.ValidationFailed>()
        failureResult.error.message shouldContain "Artifact ID cannot be empty"
        coVerify(exactly = 0) { mockSbomRepository.save(any()) }
    }

    test("createSbom - fallo por error en repositorio") {
        val request = CreateSbomRequest(
            artifactId = "artifact1", format = "CycloneDX", version = "1.4",
            components = emptyList(), relationships = emptyList()
        )
        val repoException = RuntimeException("DB error")
        coEvery { mockSbomRepository.save(any()) } returns kotlin.Result.failure(repoException)

        val result = sbomService.createSbom(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.RepositoryError>()
        coVerify(exactly = 1) { mockSbomRepository.save(any()) }
    }

    // --- getSbomById Tests ---
    test("getSbomById - exito") {
        val sbomId = "sbom123"
        coEvery { mockSbomRepository.findById(sbomId) } returns kotlin.Result.success(testSbomDocument)

        val result = sbomService.getSbomById(sbomId)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<SbomDocumentResponse>
        successResult.value.id shouldBe testSbomDocument.id
        coVerify(exactly = 1) { mockSbomRepository.findById(sbomId) }
    }

    test("getSbomById - fallo por ID vacio") {
        val result = sbomService.getSbomById("")

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.ValidationFailed>()
        failureResult.error.message shouldContain "SBOM ID cannot be empty"
        coVerify(exactly = 0) { mockSbomRepository.findById(any()) }
    }

    test("getSbomById - fallo por no encontrado") {
        val sbomId = "nonexistent"
        coEvery { mockSbomRepository.findById(sbomId) } returns kotlin.Result.failure(RuntimeException("Sbom with ID '$sbomId' not found in domain."))

        val result = sbomService.getSbomById(sbomId)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.SbomNotFound>()
        coVerify(exactly = 1) { mockSbomRepository.findById(sbomId) }
    }

    // --- analyzeSbom Tests ---
    test("analyzeSbom - exito") {
        val request = SbomAnalysisRequest(sbomId = "sbom123")
        val domainAnalysisResult = DomainAnalysisResult(mapOf("comp1" to listOf(DomainVulnerabilityInfo("CVE-123", "High", "Desc"))), mapOf("comp1" to listOf(DomainLicenseInfo("Apache-2.0", "OSI"))))
        coEvery { mockSbomRepository.findById(request.sbomId) } returns kotlin.Result.success(testSbomDocument)
        coEvery { mockSbomAnalyzerPort.analyze(testSbomDocument) } returns kotlin.Result.success(domainAnalysisResult)
        coEvery { mockSbomRepository.save(any()) } coAnswers { DomainResult.success(arg(0) as SbomDocument) }

        val result = sbomService.analyzeSbom(request)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<SbomAnalysisResponse>
        successResult.value.vulnerabilities.shouldNotBeNull()
        coVerifyAll {
            mockSbomRepository.findById(request.sbomId)
            mockSbomAnalyzerPort.analyze(testSbomDocument)
            mockSbomRepository.save(any())
            mockEventPublisher.publish(any(named("SbomAnalyzedEvent"))) )
        }
    }

    test("analyzeSbom - fallo SBOM no encontrado") {
        val request = SbomAnalysisRequest(sbomId = "nonexistent")
        coEvery { mockSbomRepository.findById(request.sbomId) } returns kotlin.Result.failure(RuntimeException("Sbom with ID '${request.sbomId}' not found in domain."))

        val result = sbomService.analyzeSbom(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.SbomNotFound>()
    }
    
    test("analyzeSbom - fallo error en el analizador") {
        val request = SbomAnalysisRequest(sbomId = "sbom123")
        coEvery { mockSbomRepository.findById(request.sbomId) } returns DomainResult.success(testSbomDocument)
        coEvery { mockSbomAnalyzerPort.analyze(testSbomDocument) } returns kotlin.Result.failure(RuntimeException("Analyzer boom"))

        val result = sbomService.analyzeSbom(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.SbomAnalysisFailed>()
    }

    // --- generateSbomFromArtifact Tests ---
    test("generateSbomFromArtifact - exito") {
        val request = GenerateSbomFromArtifactRequest(artifactId = "artifact123", format = "CycloneDX")
        val artifactBytes = "artifact content".toByteArray()
        coEvery { mockArtifactStorage.download(request.artifactId) } returns kotlin.Result.success(artifactBytes)
        coEvery { mockSbomGenerator.generate(artifactBytes, SbomFormat.CYCLONE_DX) } returns kotlin.Result.success(testSbomDocument.copy(artifactId = request.artifactId))
        coEvery { mockSbomRepository.save(any()) } coAnswers { DomainResult.success(arg(0) as SbomDocument) }

        val result = sbomService.generateSbomFromArtifact(request)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<SbomDocumentResponse>
        successResult.value.artifactId shouldBe request.artifactId
        coVerifyAll {
            mockArtifactStorage.download(request.artifactId)
            mockSbomGenerator.generate(artifactBytes, SbomFormat.CYCLONE_DX)
            mockSbomRepository.save(any())
            mockEventPublisher.publish(any<SbomCreatedEvent>())
        }
    }

    test("generateSbomFromArtifact - fallo artefacto no encontrado") {
        val request = GenerateSbomFromArtifactRequest(artifactId = "nonexistent", format = "CycloneDX")
        coEvery { mockArtifactStorage.download(request.artifactId) } returns kotlin.Result.failure(RuntimeException("Artifact with ID '${request.artifactId}' not found in domain."))

        val result = sbomService.generateSbomFromArtifact(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.ArtifactNotFound>()
    }

    test("generateSbomFromArtifact - fallo error en generador") {
        val request = GenerateSbomFromArtifactRequest(artifactId = "artifact123", format = "CycloneDX")
        val artifactBytes = "artifact content".toByteArray()
        coEvery { mockArtifactStorage.download(request.artifactId) } returns DomainResult.success(artifactBytes)
        coEvery { mockSbomGenerator.generate(artifactBytes, SbomFormat.CYCLONE_DX) } returns kotlin.Result.failure(RuntimeException("Generator boom"))

        val result = sbomService.generateSbomFromArtifact(request)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.GenerationFailed>()
    }

    // --- getSbomsByArtifactId Tests ---
    test("getSbomsByArtifactId - exito") {
        val artifactId = "artifact1"
        val sboms = listOf(testSbomDocument, testSbomDocument.copy(id = "sbom2"))
        coEvery { mockSbomRepository.findByArtifactId(artifactId) } returns kotlin.Result.success(sboms)

        val result = sbomService.getSbomsByArtifactId(artifactId)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<List<SbomDocumentResponse>>
        successResult.value.shouldHaveSize(2)
        coVerify(exactly = 1) { mockSbomRepository.findByArtifactId(artifactId) }
    }

    test("getSbomsByArtifactId - exito con lista vacia") {
        val artifactId = "artifact_empty"
        coEvery { mockSbomRepository.findByArtifactId(artifactId) } returns kotlin.Result.success(emptyList())

        val result = sbomService.getSbomsByArtifactId(artifactId)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<List<SbomDocumentResponse>>
        successResult.value.shouldBeEmpty()
    }

    // --- getLatestSbomByArtifactId Tests ---
    test("getLatestSbomByArtifactId - exito") {
        val artifactId = "artifact1"
        coEvery { mockSbomRepository.findLatestByArtifactId(artifactId) } returns kotlin.Result.success(testSbomDocument)

        val result = sbomService.getLatestSbomByArtifactId(artifactId)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<SbomDocumentResponse>
        successResult.value.id shouldBe testSbomDocument.id
        coVerify(exactly = 1) { mockSbomRepository.findLatestByArtifactId(artifactId) }
    }

    test("getLatestSbomByArtifactId - fallo por no encontrado") {
        val artifactId = "nonexistent"
        coEvery { mockSbomRepository.findLatestByArtifactId(artifactId) } returns kotlin.Result.failure(RuntimeException("Latest SBOM for artifact ID '$artifactId' not found in domain."))

        val result = sbomService.getLatestSbomByArtifactId(artifactId)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.SbomNotFound>()
    }

    // --- findSbomsByComponent Tests ---
    val componentNameForSearch = "SearchComponent"
    val componentVersionForSearch = "1.0"

    test("findSbomsByComponent - exito con nombre y version") {
        val sboms = listOf(testSbomDocument)
        coEvery { mockSbomRepository.findByComponent(componentNameForSearch, componentVersionForSearch) } returns kotlin.Result.success(sboms)

        val result = sbomService.findSbomsByComponent(componentNameForSearch, componentVersionForSearch)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<List<SbomDocumentResponse>>
        successResult.value.shouldHaveSize(1)
        coVerify(exactly = 1) { mockSbomRepository.findByComponent(componentNameForSearch, componentVersionForSearch) }
    }

    test("findSbomsByComponent - exito solo con nombre") {
        val sboms = listOf(testSbomDocument)
        coEvery { mockSbomRepository.findByComponent(componentNameForSearch, null) } returns kotlin.Result.success(sboms)

        val result = sbomService.findSbomsByComponent(componentNameForSearch, null)

        result.isSuccess shouldBe true
        val successResult = result as SharedAppResult.Success<List<SbomDocumentResponse>>
        successResult.value.shouldHaveSize(1)
        coVerify(exactly = 1) { mockSbomRepository.findByComponent(componentNameForSearch, null) }
    }

    test("findSbomsByComponent - fallo por nombre de componente vacio") {
        val result = sbomService.findSbomsByComponent("", componentVersionForSearch)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.ValidationFailed>()
        failureResult.error.message shouldContain "Component name cannot be empty"
        coVerify(exactly = 0) { mockSbomRepository.findByComponent(any(),isNull(true)) }
    }
    
    test("findSbomsByComponent - fallo por error en repositorio (con version)") {
        val repoException = RuntimeException("DB error")
        coEvery { mockSbomRepository.findByComponent(componentNameForSearch, componentVersionForSearch) } returns kotlin.Result.failure(repoException)

        val result = sbomService.findSbomsByComponent(componentNameForSearch, componentVersionForSearch)

        result.isFailure shouldBe true
        val failureResult = result as SharedAppResult.Failure<SbomError>
        failureResult.error.shouldBeInstanceOf<SbomError.RepositoryError>()
        coVerify(exactly = 1) { mockSbomRepository.findByComponent(componentNameForSearch, componentVersionForSearch) }
    }
})