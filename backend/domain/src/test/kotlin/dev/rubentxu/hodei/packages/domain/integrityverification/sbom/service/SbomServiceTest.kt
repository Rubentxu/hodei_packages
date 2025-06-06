package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.service

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomComponent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomRepository
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * Unit tests for [dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomService].
 * These tests verify the behavior of the SbomService, particularly its interaction
 * with the SbomRepository, and the overall success/failure flows.
 */
class SbomServiceTest : FunSpec({
    val mockSbomRepository = mockk<SbomRepository>()
    val sbomService = object : SbomService {
        override suspend fun saveSbom(sbomDocument: SbomDocument): Result<SbomDocument> =
            mockSbomRepository.save(sbomDocument)

        override suspend fun findById(id: String): Result<SbomDocument?> = mockSbomRepository.findById(id)
        override suspend fun findByArtifactId(artifactId: String): Result<List<SbomDocument>> =
            mockSbomRepository.findByArtifactId(artifactId)

        override suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat?): Result<SbomDocument?> =
            mockSbomRepository.findLatestByArtifactId(artifactId, format)

        override suspend fun findByComponent(
            componentName: String,
            componentVersion: String?
        ): Result<List<SbomDocument>> = mockSbomRepository.findByComponent(componentName, componentVersion)
    }

    val testSbomComponent = SbomComponent(
        name = "org.example:lib",
        version = "1.0.0",
        type = "library"
    )
    val testSbomDocument = SbomDocument(
        artifactId = "artifact-1",
        format = SbomFormat.CYCLONE_DX,
        components = listOf(testSbomComponent),
        specVersion = "1.4",
        creationTime = Instant.now()
    )

    test("saveSbom should delegate to repository and return saved document") {
        coEvery { mockSbomRepository.save(testSbomDocument) } returns Result.success(testSbomDocument)
        val result = runBlocking { sbomService.saveSbom(testSbomDocument) }
        result.getOrNull() shouldBe testSbomDocument
    }

    test("saveSbom should handle repository failure and return error") {
        val errorMessage = "Failed to save SBOM"
        coEvery { mockSbomRepository.save(testSbomDocument) } returns Result.failure(RuntimeException(errorMessage))
        val result = runBlocking { sbomService.saveSbom(testSbomDocument) }
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }

    test("findById should delegate to repository and return document") {
        coEvery { mockSbomRepository.findById("sbom-1") } returns Result.success(testSbomDocument)
        val result = runBlocking { sbomService.findById("sbom-1") }
        result.getOrNull() shouldBe testSbomDocument
    }

    test("findById should return null when repository returns null") {
        coEvery { mockSbomRepository.findById("sbom-1") } returns Result.success(null)
        val result = runBlocking { sbomService.findById("sbom-1") }
        result.getOrNull() shouldBe null
    }

    test("findById should handle repository failure and return error") {
        val errorMessage = "Failed to find SBOM"
        coEvery { mockSbomRepository.findById("sbom-1") } returns Result.failure(RuntimeException(errorMessage))
        val result = runBlocking { sbomService.findById("sbom-1") }
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }

    test("findByArtifactId should delegate to repository and return list") {
        coEvery { mockSbomRepository.findByArtifactId("artifact-1") } returns Result.success(listOf(testSbomDocument))
        val result = runBlocking { sbomService.findByArtifactId("artifact-1") }
        result.getOrNull() shouldBe listOf(testSbomDocument)
    }

    test("findByArtifactId should handle repository failure and return error") {
        val errorMessage = "Failed to find SBOMs by artifact ID"
        coEvery { mockSbomRepository.findByArtifactId("artifact-1") } returns Result.failure(
            RuntimeException(
                errorMessage
            )
        )
        val result = runBlocking { sbomService.findByArtifactId("artifact-1") }
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }

    test("findLatestByArtifactId should delegate to repository and return latest document") {
        coEvery {
            mockSbomRepository.findLatestByArtifactId(
                "artifact-1",
                SbomFormat.CYCLONE_DX
            )
        } returns Result.success(testSbomDocument)
        val result = runBlocking { sbomService.findLatestByArtifactId("artifact-1", SbomFormat.CYCLONE_DX) }
        result.getOrNull() shouldBe testSbomDocument
    }

    test("findLatestByArtifactId should return null when repository returns null") {
        coEvery {
            mockSbomRepository.findLatestByArtifactId(
                "artifact-1",
                SbomFormat.CYCLONE_DX
            )
        } returns Result.success(null)
        val result = runBlocking { sbomService.findLatestByArtifactId("artifact-1", SbomFormat.CYCLONE_DX) }
        result.getOrNull() shouldBe null
    }

    test("findLatestByArtifactId should handle repository failure and return error") {
        val errorMessage = "Failed to find latest SBOM by artifact ID"
        coEvery {
            mockSbomRepository.findLatestByArtifactId(
                "artifact-1",
                SbomFormat.CYCLONE_DX
            )
        } returns Result.failure(RuntimeException(errorMessage))
        val result = runBlocking { sbomService.findLatestByArtifactId("artifact-1", SbomFormat.CYCLONE_DX) }
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }

    test("findByComponent should delegate to repository and return list") {
        coEvery { mockSbomRepository.findByComponent("openssl", null) } returns Result.success(listOf(testSbomDocument))
        val result = runBlocking { sbomService.findByComponent("openssl", null) }
        result.getOrNull() shouldBe listOf(testSbomDocument)
    }

    test("findByComponent should handle repository failure and return error") {
        val errorMessage = "Failed to find SBOMs by component"
        coEvery { mockSbomRepository.findByComponent("openssl", null) } returns Result.failure(
            RuntimeException(
                errorMessage
            )
        )
        val result = runBlocking { sbomService.findByComponent("openssl", null) }
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe errorMessage
    }
})