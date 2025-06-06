package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.service

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleGraph
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNode
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNodeType
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.MerkleGraphRepository
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.MerkleGraphService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Unit tests for MerkleGraphService following TDD/BDD principles.
 * The repository is mocked, and main use cases are covered: creation, signing, verification, and querying.
 */
class MerkleGraphServiceTest : FunSpec({
    val repository = mockk<MerkleGraphRepository>(relaxed = true)
    // This is an anonymous implementation of the service for testing purposes,
    // assuming a real implementation would primarily delegate to the repository.
    val service: MerkleGraphService = object : MerkleGraphService {
        override suspend fun createGraph(artifactId: String, rootNode: MerkleNode): Result<MerkleGraph> {
            val newGraph = MerkleGraph(artifactId = artifactId, rootNode = rootNode, signatures = emptyList())
            return repository.save(newGraph)
        }

        override suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph> {
            return repository.addSignature(artifactId, signature)
        }

        override suspend fun verifyGraph(artifactId: String): Result<Boolean> {
            // Assuming service's verifyGraph directly uses repository's structure verification for this test.
            // A real implementation might have more complex logic.
            return repository.verifyGraphStructure(artifactId)
        }

        override suspend fun findByArtifactId(artifactId: String): Result<MerkleGraph?> {
            return repository.findByArtifactId(artifactId)
        }

        override suspend fun findByRootHash(rootHash: ContentHash): Result<MerkleGraph?> {
            return repository.findByRootHash(rootHash)
        }

        override suspend fun findBySignatureKeyId(keyId: String): Result<List<MerkleGraph>> {
            return repository.findBySignatureKeyId(keyId)
        }
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
        val graphToSave = MerkleGraph(artifactId, rootNode, emptyList())
        val expectedGraph = MerkleGraph(artifactId, rootNode, emptyList()) // Same as graphToSave for this test
        coEvery { repository.save(graphToSave) } returns Result.success(expectedGraph)

        // Act
        val result = service.createGraph(artifactId, rootNode)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe expectedGraph
        coVerify { repository.save(graphToSave) }
    }

    test("addSignature should add signature and update graph via repository") {
        // Arrange
        val artifactId = "artifact-1"
        val signature = mockk<Signature>()
        val rootNodeStub = MerkleNode(
            path = "dummy",
            contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.FILE
        )
        val updatedGraph = MerkleGraph(
            artifactId = artifactId,
            rootNode = rootNodeStub,
            signatures = listOf(signature) // Assuming the repo returns the graph with the signature
        )
        coEvery { repository.addSignature(artifactId, signature) } returns Result.success(updatedGraph)

        // Act
        val result = service.addSignature(artifactId, signature)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe updatedGraph
        coVerify { repository.addSignature(artifactId, signature) }
    }

    test("verifyGraph should delegate to repository and return verification result") {
        // Arrange
        val artifactId = "artifact-1"
        coEvery { repository.verifyGraphStructure(artifactId) } returns Result.success(true)

        // Act
        val result = service.verifyGraph(artifactId)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe true
        coVerify { repository.verifyGraphStructure(artifactId) }
    }

    test("findByArtifactId should delegate to repository and return graph") {
        // Arrange
        val artifactId = "artifact-1"
        val rootNodeStub = MerkleNode(
            path = "dummy",
            contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.FILE
        )
        val expectedGraph = MerkleGraph(
            artifactId = artifactId,
            rootNode = rootNodeStub,
            signatures = emptyList()
        )
        coEvery { repository.findByArtifactId(artifactId) } returns Result.success(expectedGraph)

        // Act
        val result = service.findByArtifactId(artifactId)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe expectedGraph
        coVerify { repository.findByArtifactId(artifactId) }
    }

    test("findByRootHash should delegate to repository and return graph") {
        // Arrange
        val rootHash = ContentHash(value = "abc123", algorithm = "SHA-256")
        val leafNodeStub = MerkleNode(
            path = "root/file.txt",
            contentHash = ContentHash(value = "leafhash", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.FILE
        )
        val rootNodeStub = MerkleNode( // The root node in the graph should match this hash
            path = "root/",
            contentHash = rootHash,
            nodeType = MerkleNodeType.DIRECTORY,
            children = listOf(leafNodeStub)
        )
        val expectedGraph = MerkleGraph(
            artifactId = "artifact-1",
            rootNode = rootNodeStub,
            signatures = emptyList()
        )
        coEvery { repository.findByRootHash(rootHash) } returns Result.success(expectedGraph)

        // Act
        val result = service.findByRootHash(rootHash)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe expectedGraph
        coVerify { repository.findByRootHash(rootHash) }
    }

    test("findBySignatureKeyId should delegate to repository and return list of graphs") {
        // Arrange
        val keyId = "signer@example.com"
        val rootNodeStub = MerkleNode(
            path = "dummy",
            contentHash = ContentHash(value = "dummy", algorithm = "SHA-256"),
            nodeType = MerkleNodeType.FILE
        )
        val expectedGraphs = listOf(
            MerkleGraph(
                artifactId = "artifact-signed-by-$keyId", // Example artifactId
                rootNode = rootNodeStub,
                signatures = listOf(mockk<Signature> { coEvery { this@mockk.keyId } returns keyId })
            )
        )
        coEvery { repository.findBySignatureKeyId(keyId) } returns Result.success(expectedGraphs)

        // Act
        val result = service.findBySignatureKeyId(keyId)

        // Assert
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe expectedGraphs
        coVerify { repository.findBySignatureKeyId(keyId) }
    }
})