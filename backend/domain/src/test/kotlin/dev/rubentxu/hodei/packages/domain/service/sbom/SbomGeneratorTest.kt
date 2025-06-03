package dev.rubentxu.hodei.packages.domain.service.sbom

import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationCompletedEvent
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationFailedEvent
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationStartedEvent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.repository.sbom.SbomRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.clearMocks
import io.mockk.mockk

class SbomGeneratorTest : StringSpec({
    val mockEventPublisher = mockk<EventPublisher>(relaxed = true)
    val mockSbomRepository = mockk<SbomRepository>()
    val artifactId = "test-artifact-123"
    val format = SbomFormat.CYCLONE_DX

    val sbomGenerator = SbomGenerator(mockEventPublisher, mockSbomRepository)

    "generateSbom should emit start event and extract components from artifactContent" {
        val artifactContent = """
            {
                "name": "test-package",
                "version": "1.0.0",
                "dependencies": {
                    "dependency1": "1.2.3",
                    "dependency2": "4.5.6"
                }
            }
        """.trimIndent().toByteArray()

        val expectedComponents = listOf(
            SbomComponent(name = "test-package", version = "1.0.0", type = "library"),
            SbomComponent(name = "dependency1", version = "1.2.3", type = "library"),
            SbomComponent(name = "dependency2", version = "4.5.6", type = "library")
        )

        val generatedSbomDoc = SbomDocument(
            artifactId = artifactId,
            format = format,
            components = expectedComponents
        )

        // Configure repository mock
        coEvery { mockSbomRepository.save(any()) } returns Result.success(generatedSbomDoc)

        // Execute the method under test
        val result = sbomGenerator.generateSbom(artifactId, artifactContent, format)

        // Verify the result
        result.isSuccess shouldBe true
        val sbomDoc = result.getOrNull()
        sbomDoc shouldNotBe null
        sbomDoc!!.artifactId shouldBe artifactId
        sbomDoc.format shouldBe format
        sbomDoc.components.size shouldBe 3

        // Verify event publishing
        coVerify(exactly = 1) { mockEventPublisher.publish(match { it is SbomGenerationStartedEvent }) }
        coVerify(exactly = 1) { mockEventPublisher.publish(match { it is SbomGenerationCompletedEvent }) }
        coVerify(exactly = 1) { mockSbomRepository.save(any()) }
    }

    "generateSbom should handle errors and emit failure event" {
        // Limpiar mocks para evitar interferencias entre tests
        clearMocks(mockEventPublisher)

        val artifactContent = """
        {
            "name": "test-package",
            "version": "1.0.0"
        }
    """.trimIndent().toByteArray()
        val errorMessage = "Failed to save SBOM document"
        val exception = RuntimeException(errorMessage)

        // Configurar el repositorio para que falle (esto es lo que faltaba)
        coEvery { mockSbomRepository.save(any()) } throws exception

        // Ejecutar el método bajo prueba
        val result = sbomGenerator.generateSbom(artifactId, artifactContent, format)

        // Verificar que el resultado sea un fallo
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage

        // Verificar que se publicaron los eventos correctos en orden
        coVerifyOrder {
            mockEventPublisher.publish(match { it is SbomGenerationStartedEvent && it.artifactId == artifactId })
            mockEventPublisher.publish(match { it is SbomGenerationFailedEvent && it.artifactId == artifactId })
        }
    }
})
