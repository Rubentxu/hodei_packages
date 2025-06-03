package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.CreateSbomRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomComponentDto
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomDocumentResponse
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomRelationshipDto
import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomRelationship
import dev.rubentxu.hodei.packages.domain.repository.ArtifactStoragePort
import dev.rubentxu.hodei.packages.domain.repository.sbom.SbomRepository
import dev.rubentxu.hodei.packages.domain.service.sbom.SbomGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

class SbomServiceImplTest : FunSpec({
    
    // Mocks de los servicios y repositorios necesarios
    val mockSbomRepository = mockk<SbomRepository>()
    val mockEventPublisher = mockk<EventPublisher>(relaxed = true)
    val mockSbomGenerator = mockk<SbomGenerator>()
    val mockArtifactStorage = mockk<ArtifactStoragePort>()
    
    // Servicio a probar
    val sbomService = SbomServiceImpl(
        sbomRepository = mockSbomRepository,
        sbomGenerator = mockSbomGenerator,
        eventPublisher = mockEventPublisher,
        artifactStorage = mockArtifactStorage
    )
    
    // Datos de prueba comunes
    val artifactId = "test-artifact-123"
    val sbomId = "test-sbom-456"
    val now = Instant.now()
    
    val testComponent = SbomComponent(
        name = "testLib",
        version = "1.0.0",
        type = "library",
        supplier = "Test Supplier"
    )
    
    val testRelationship = SbomRelationship(
        fromComponentId = "comp1",
        toComponentId = "comp2",
        type = "DEPENDS_ON"
    )
    
    val testDocument = SbomDocument(
        id = sbomId,
        artifactId = artifactId,
        format = SbomFormat.CYCLONE_DX,
        version = "1.0.0",
        creationTime = now,
        components = listOf(testComponent),
        relationships = listOf(testRelationship)
    )
    
    test("createSbom should store a new SBOM document") {
        // Preparar
        val request = CreateSbomRequest(
            artifactId = artifactId,
            format = "CYCLONE_DX",
            version = "1.0.0",
            components = listOf(
                SbomComponentDto(
                    name = "testLib",
                    version = "1.0.0",
                    type = "library",
                    supplier = "Test Supplier"
                )
            ),
            relationships = listOf(
                SbomRelationshipDto(
                    fromComponentId = "comp1",
                    toComponentId = "comp2",
                    type = "DEPENDS_ON"
                )
            )
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
        
        // Verificar que el documento pasado al repositorio tiene los valores correctos
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
            mockArtifactStorage.getArtifactContent(artifactId)
        } returns Result.success(artifactContent)
        
        coEvery { 
            mockSbomGenerator.generateSbom(artifactId, artifactContent, SbomFormat.CYCLONE_DX)
        } returns Result.success(testDocument)
        
        // Ejecutar
        val result = sbomService.generateSbomFromArtifact(artifactId, format)
        
        // Verificar
        result.isSuccess shouldBe true
        val response = result.getOrNull()
        response shouldNotBe null
        response!!.artifactId shouldBe artifactId
        
        coVerify(exactly = 1) { mockArtifactStorage.getArtifactContent(artifactId) }
        coVerify(exactly = 1) { 
            mockSbomGenerator.generateSbom(artifactId, artifactContent, SbomFormat.CYCLONE_DX)
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
        response!!.id shouldBe sbomId
        response.artifactId shouldBe artifactId
        
        coVerify(exactly = 1) { mockSbomRepository.findById(sbomId) }
    }
    
    test("getSbomsByArtifactId should return all SBOM documents for an artifact") {
        // Preparar
        val document1 = testDocument.copy(id = "sbom1", version = "1.0.0")
        val document2 = testDocument.copy(id = "sbom2", version = "1.1.0")
        
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
})
