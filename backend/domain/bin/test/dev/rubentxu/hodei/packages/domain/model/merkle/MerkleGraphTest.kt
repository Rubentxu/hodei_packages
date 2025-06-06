package dev.rubentxu.hodei.packages.domain.model.merkle

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleGraph
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNode
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MerkleGraphTest : StringSpec({
    val contentHash = ContentHash.create("test content")
    val childNode1 = MerkleNode("path/to/file1.txt", ContentHash.create("content1"))
    val childNode2 = MerkleNode("path/to/file2.txt", ContentHash.create("content2"))
    val rootNode = MerkleNode.computeHash("path/to", listOf(childNode1, childNode2))
    
    "MerkleGraph should be created with required fields" {
        val graph = MerkleGraph(
            artifactId = "test-artifact-123",
            rootNode = rootNode
        )
        
        graph.artifactId shouldBe "test-artifact-123"
        graph.rootNode shouldBe rootNode
        graph.rootHash shouldBe rootNode.contentHash
        graph.signatures shouldBe emptyList()
    }
    
    "MerkleGraph should support signatures" {
        val signature = Signature(
            value = "test-signature",
            algorithm = "Ed25519",
            contentHash = rootNode.contentHash,
            keyId = "user@example.com"
        )
        
        val graph = MerkleGraph(
            artifactId = "test-artifact-123",
            rootNode = rootNode,
            signatures = listOf(signature)
        )
        
        graph.signatures.size shouldBe 1
        graph.signatures.first() shouldBe signature
    }
    
    "MerkleGraph should validate artifactId is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            MerkleGraph(
                artifactId = "",
                rootNode = rootNode
            )
        }
        
        exception.message shouldBe "ArtifactId cannot be blank"
    }
    
    "MerkleGraph should provide rootHash based on rootNode's contentHash" {
        val graph = MerkleGraph(
            artifactId = "test-artifact-123",
            rootNode = rootNode
        )
        
        graph.rootHash shouldBe rootNode.contentHash
    }
    
    "MerkleGraph.addSignature should add a new signature" {
        val graph = MerkleGraph(
            artifactId = "test-artifact-123",
            rootNode = rootNode
        )
        
        val signature = Signature(
            value = "test-signature",
            algorithm = "Ed25519",
            contentHash = rootNode.contentHash,
            keyId = "user@example.com"
        )
        
        val updatedGraph = graph.addSignature(signature)
        
        updatedGraph shouldNotBe graph
        updatedGraph.signatures.size shouldBe 1
        updatedGraph.signatures.first() shouldBe signature
    }
    
    "MerkleGraph.addSignature should validate signature contentHash matches rootHash" {
        val graph = MerkleGraph(
            artifactId = "test-artifact-123",
            rootNode = rootNode
        )
        
        val signature = Signature(
            value = "test-signature",
            algorithm = "Ed25519",
            contentHash = ContentHash.create("different content"),
            keyId = "user@example.com"
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            graph.addSignature(signature)
        }
        
        exception.message shouldBe "Signature contentHash must match graph rootHash"
    }
})
