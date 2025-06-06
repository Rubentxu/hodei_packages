package dev.rubentxu.hodei.packages.domain.service.merkle

import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleGraph
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNode
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNodeType
import dev.rubentxu.hodei.packages.domain.model.merkle.Signature
import dev.rubentxu.hodei.packages.domain.ports.merkle.MerkleGraphService
import dev.rubentxu.hodei.packages.domain.ports.merkle.MerkleGraphRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Pruebas unitarias para MerkleGraphService siguiendo TDD/BDD.
 * Se mockea el repositorio y se cubren los principales casos de uso: creación, firma, verificación y consulta.
 */
class MerkleGraphServiceTest : FunSpec({
    val repository = mockk<MerkleGraphRepository>(relaxed = true)
    val service = object : MerkleGraphService {
        override suspend fun createGraph(artifactId: String, rootNode: MerkleNode): Result<MerkleGraph> = TODO()
        override suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph> = TODO()
        override suspend fun verifyGraph(artifactId: String): Result<Boolean> = TODO()
        override suspend fun findByArtifactId(artifactId: String): Result<MerkleGraph?> = TODO()
        override suspend fun findByRootHash(rootHash: ContentHash): Result<MerkleGraph?> = TODO()
        override suspend fun findBySignatureKeyId(keyId: String): Result<List<MerkleGraph>> = TODO()
    }

    test("createGraph should delegate to repository and return created graph") {
        // Arrange
        val artifactId = "artifact-1"
        val leafNode = MerkleNode(
            path = "root/file.txt",
            contentHash = ContentHash(value = "abc123", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.FILE
        )
        val rootNode = MerkleNode(
            path = "root/",
            contentHash = ContentHash(value = "def456", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.DIRECTORY,
            children = listOf(leafNode)
        )
        val expectedGraph = MerkleGraph(artifactId, rootNode, emptyList())
        coEvery { repository.save(any()) } returns Result.success(expectedGraph)
        // Act (cuando se implemente)
        // val result = service.createGraph(artifactId, rootNode)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe expectedGraph
        // coVerify { repository.save(any()) }
    }

    test("addSignature should add signature and update graph via repository") {
        // Arrange
        val artifactId = "artifact-1"
        val signature = mockk<Signature>()
        val updatedGraph = MerkleGraph(
            artifactId = artifactId,
            rootNode = MerkleNode(
                path = "dummy",
                contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
                nodeType = MerkleNodeType.FILE
            ),
            signatures = emptyList()
        )
        coEvery { repository.addSignature(artifactId, signature) } returns Result.success(updatedGraph)
        // Act (cuando se implemente)
        // val result = service.addSignature(artifactId, signature)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe updatedGraph
        // coVerify { repository.addSignature(artifactId, signature) }
    }

    test("verifyGraph should delegate to repository and return verification result") {
        val artifactId = "artifact-1"
        coEvery { repository.verifyGraphStructure(artifactId) } returns Result.success(true)
        // Act (cuando se implemente)
        // val result = service.verifyGraph(artifactId)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe true
        // coVerify { repository.verifyGraphStructure(artifactId) }
    }

    test("findByArtifactId should delegate to repository and return graph") {
        val artifactId = "artifact-1"
        val expectedGraph = MerkleGraph(
            artifactId = artifactId,
            rootNode = MerkleNode(
                path = "dummy",
                contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
                nodeType = MerkleNodeType.FILE
            ),
            signatures = emptyList()
        )
        coEvery { repository.findByArtifactId(artifactId) } returns Result.success(expectedGraph)
        // Act (cuando se implemente)
        // val result = service.findByArtifactId(artifactId)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe expectedGraph
        // coVerify { repository.findByArtifactId(artifactId) }
    }

    test("findByRootHash should delegate to repository and return graph") {
        val rootHash = ContentHash(value = "abc123", algorithm = "SHA-256")
        val expectedGraph = MerkleGraph(
            artifactId = "artifact-1",
            rootNode = MerkleNode(
                path = "dummy",
                contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
                nodeType = MerkleNodeType.FILE
            ),
            signatures = emptyList()
        )
        coEvery { repository.findByRootHash(rootHash) } returns Result.success(expectedGraph)
        // Act (cuando se implemente)
        // val result = service.findByRootHash(rootHash)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe expectedGraph
        // coVerify { repository.findByRootHash(rootHash) }
    }

    test("findBySignatureKeyId should delegate to repository and return list of graphs") {
        val keyId = "signer@example.com"
        val expectedGraphs = listOf(MerkleGraph(
            artifactId = keyId,
            rootNode = MerkleNode(
                path = "dummy",
                contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
                nodeType = MerkleNodeType.FILE
            ),
            signatures = emptyList()
        ))
        coEvery { repository.findBySignatureKeyId(keyId) } returns Result.success(expectedGraphs)
        // Act (cuando se implemente)
        // val result = service.findBySignatureKeyId(keyId)
        // Assert (cuando se implemente)
        // result.getOrNull() shouldBe expectedGraphs
    }
})
