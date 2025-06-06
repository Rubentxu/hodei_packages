package dev.rubentxu.hodei.packages.domain.service.sbom

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomService
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class SbomServiceTest : FunSpec({
    val repository = mockk<SbomRepository>()
    val service = object : SbomService {
        override suspend fun saveSbom(sbomDocument: SbomDocument) = repository.save(sbomDocument)
        override suspend fun findById(id: String) = repository.findById(id)
        override suspend fun findByArtifactId(artifactId: String) = repository.findByArtifactId(artifactId)
        override suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat?) = repository.findLatestByArtifactId(artifactId, format)
        override suspend fun findByComponent(componentName: String, componentVersion: String?) = repository.findByComponent(componentName, componentVersion)
    }

    val dummyComponent = dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent(
        name = "org.example:lib",
        version = "1.0.0",
        type = "library"
    )
    val dummySbom = SbomDocument(
        artifactId = "artifact-1",
        format = SbomFormat.CYCLONE_DX,
        components = listOf(dummyComponent),
        version = "1.0",
        creationTime = java.time.Instant.now()
    )

    test("saveSbom should delegate to repository and return saved document") {
        coEvery { repository.save(dummySbom) } returns Result.success(dummySbom)
        val result = runBlocking { service.saveSbom(dummySbom) }
        result.getOrNull() shouldBe dummySbom
    }

    test("findById should delegate to repository and return document") {
        coEvery { repository.findById("sbom-1") } returns Result.success(dummySbom)
        val result = runBlocking { service.findById("sbom-1") }
        result.getOrNull() shouldBe dummySbom
    }

    test("findByArtifactId should delegate to repository and return list") {
        coEvery { repository.findByArtifactId("artifact-1") } returns Result.success(listOf(dummySbom))
        val result = runBlocking { service.findByArtifactId("artifact-1") }
        result.getOrNull() shouldBe listOf(dummySbom)
    }

    test("findLatestByArtifactId should delegate to repository and return latest document") {
        coEvery { repository.findLatestByArtifactId("artifact-1", SbomFormat.CYCLONE_DX) } returns Result.success(dummySbom)
        val result = runBlocking { service.findLatestByArtifactId("artifact-1", SbomFormat.CYCLONE_DX) }
        result.getOrNull() shouldBe dummySbom
    }

    test("findByComponent should delegate to repository and return list") {
        coEvery { repository.findByComponent("openssl", null) } returns Result.success(listOf(dummySbom))
        val result = runBlocking { service.findByComponent("openssl", null) }
        result.getOrNull() shouldBe listOf(dummySbom)
    }
})
