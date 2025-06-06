package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MerkleNodeTest : StringSpec({
    val contentHash = ContentHash("SHA-256", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    
    "MerkleNode should be created with required fields" {
        val node = MerkleNode(
            path = "path/to/file.txt",
            contentHash = contentHash
        )
        
        node.path shouldBe "path/to/file.txt"
        node.contentHash shouldBe contentHash
        node.nodeType shouldBe MerkleNodeType.FILE
        node.children shouldBe emptyList()
    }
    
    "MerkleNode should support directory type with children" {
        val childNode1 = MerkleNode(
            path = "path/to/dir/file1.txt",
            contentHash = contentHash
        )
        
        val childNode2 = MerkleNode(
            path = "path/to/dir/file2.txt",
            contentHash = contentHash
        )
        
        val dirNode = MerkleNode(
            path = "path/to/dir",
            contentHash = contentHash,
            nodeType = MerkleNodeType.DIRECTORY,
            children = listOf(childNode1, childNode2)
        )
        
        dirNode.path shouldBe "path/to/dir"
        dirNode.nodeType shouldBe MerkleNodeType.DIRECTORY
        dirNode.children.size shouldBe 2
        dirNode.children shouldContain childNode1
        dirNode.children shouldContain childNode2
    }
    
    "MerkleNode should validate path is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            MerkleNode(
                path = "",
                contentHash = contentHash
            )
        }
        
        exception.message shouldBe "Node path cannot be blank"
    }
    
    "MerkleNode.computeHash should create a node with hash derived from child hashes" {
        val childNode1 = MerkleNode(
            path = "path/to/dir/file1.txt",
            contentHash = ContentHash.create("content1")
        )
        
        val childNode2 = MerkleNode(
            path = "path/to/dir/file2.txt",
            contentHash = ContentHash.create("content2")
        )
        
        val dirNode = MerkleNode.computeHash(
            path = "path/to/dir",
            children = listOf(childNode1, childNode2),
            algorithm = "SHA-256"
        )
        
        dirNode.shouldBeInstanceOf<MerkleNode>()
        dirNode.path shouldBe "path/to/dir"
        dirNode.nodeType shouldBe MerkleNodeType.DIRECTORY
        dirNode.children shouldBe listOf(childNode1, childNode2)
        // El hash debe ser derivado de los hashes de sus hijos
        dirNode.contentHash shouldNotBe childNode1.contentHash
        dirNode.contentHash shouldNotBe childNode2.contentHash
    }
    
    "MerkleNode.computeHash should validate children list is not empty" {
        val exception = shouldThrow<IllegalArgumentException> {
            MerkleNode.computeHash(
                path = "path/to/dir",
                children = emptyList()
            )
        }
        
        exception.message shouldBe "Children list cannot be empty for hash computation"
    }
})
