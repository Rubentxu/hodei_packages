package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.service

import dev.rubentxu.hodei.packages.domain.common.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.MerkleGraphVerifiedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.MerkleVerificationFailedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.TamperingDetectedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNode
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNodeType
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.ContentAddressableStorage
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.CryptographicService
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.MerkleGraphRepository

/**
 * Domain service responsible for verifying the integrity and authenticity of artifacts
 * using Merkle graphs and cryptographic signatures.
 *
 * @param eventPublisher Publisher for domain events
 * @param merkleGraphRepository Repository for accessing stored Merkle graphs
 * @param cryptographicService Service for cryptographic operations
 * @param contentStorage Content-addressable storage
 */
class MerkleVerificationService(
    private val eventPublisher: EventPublisher,
    private val merkleGraphRepository: MerkleGraphRepository,
    private val cryptographicService: CryptographicService,
    private val contentStorage: ContentAddressableStorage
) {
    /**
     * Verifies the integrity and authenticity of a complete artifact.
     * Checks both the structural validity of the Merkle graph and
     * the validity of the signatures.
     *
     * @param artifactId ID of the artifact to verify
     * @return Result encapsulating true if valid, false if not, or an error
     */
    suspend fun verifyArtifact(artifactId: String): Result<Boolean> {
        return try {
            // Retrieve the Merkle graph of the artifact
            val graphResult = merkleGraphRepository.findByArtifactId(artifactId)
            if (graphResult.isFailure) {
                return Result.failure(
                    graphResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve Merkle graph for artifact $artifactId")
                )
            }

            val graph = graphResult.getOrNull()
            if (graph == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = null,
                    reason = "Merkle graph not found for artifact"
                )
                return Result.success(false)
            }

            // Verify the structure of the graph
            val structureResult = merkleGraphRepository.verifyGraphStructure(artifactId)
            if (structureResult.isFailure) {
                return Result.failure(
                    structureResult.exceptionOrNull()
                        ?: IllegalStateException("Error during graph structure verification")
                )
            }

            val isStructureValid = structureResult.getOrNull() ?: false
            if (!isStructureValid) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Invalid Merkle graph structure"
                )
                return Result.success(false)
            }

            // Verify signatures
            val verifiedSignatures = mutableListOf<String>()
            var allSignaturesValid = true

            for (signature in graph.signatures) {
                val signatureResult = cryptographicService.verify(
                    signature = signature,
                    contentHash = graph.rootHash
                )

                if (signatureResult.isFailure) {
                    return Result.failure(
                        signatureResult.exceptionOrNull()
                            ?: IllegalStateException("Error during signature verification")
                    )
                }

                val isSignatureValid = signatureResult.getOrNull() ?: false
                if (isSignatureValid) {
                    verifiedSignatures.add(signature.keyId)
                } else {
                    allSignaturesValid = false
                }
            }

            if (!allSignaturesValid || verifiedSignatures.isEmpty()) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Invalid signature"
                )
                return Result.success(false)
            }

            // If we get here, everything is valid
            eventPublisher.publish(
                MerkleGraphVerifiedEvent(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    verifiedSignatures = verifiedSignatures
                )
            )

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies the integrity of a specific file within an artifact.
     * Compares the current content hash with the hash recorded in the Merkle graph.
     *
     * @param artifactId ID of the artifact containing the file
     * @param filePath Path of the file to verify
     * @return Result encapsulating true if valid, false if not, or an error
     */
    suspend fun verifyArtifactContent(artifactId: String, filePath: String): Result<Boolean> {
        return try {
            // Retrieve the Merkle graph
            val graphResult = merkleGraphRepository.findByArtifactId(artifactId)
            if (graphResult.isFailure || graphResult.getOrNull() == null) {
                return Result.failure(
                    graphResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve Merkle graph for artifact $artifactId")
                )
            }

            val graph = graphResult.getOrNull()!!

            // Find the node corresponding to the file path
            val fileNode = findNodeByPath(graph.rootNode, filePath)
            if (fileNode == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "File not found in Merkle graph",
                    failureLocation = filePath
                )
                return Result.success(false)
            }

            // Get the current content of the file
            val expectedHash = fileNode.contentHash
            val contentResult = contentStorage.retrieve(expectedHash)

            if (contentResult.isFailure) {
                return Result.failure(
                    contentResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve content for verification")
                )
            }

            val content = contentResult.getOrNull()
            if (content == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Content not found in storage",
                    failureLocation = filePath
                )
                return Result.success(false)
            }

            // Calculate the current content hash
            val actualHash = ContentHash.create(content, algorithm = expectedHash.algorithm)

            // Compare hashes
            val isValid = actualHash == expectedHash

            if (!isValid) {
                // Tampering detected
                eventPublisher.publish(
                    TamperingDetectedEvent(
                        artifactId = artifactId,
                        expectedHash = expectedHash,
                        actualHash = actualHash,
                        nodePath = filePath
                    )
                )
            }

            Result.success(isValid)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches for a node in the Merkle graph by its path.
     */
    private fun findNodeByPath(node: MerkleNode, path: String): MerkleNode? {
        if (node.path == path) {
            return node
        }

        if (node.nodeType == MerkleNodeType.DIRECTORY) {
            for (child in node.children) {
                val result = findNodeByPath(child, path)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * Publishes a verification failure event.
     */
    private suspend fun publishVerificationFailed(
        artifactId: String,
        rootHash: ContentHash?,
        reason: String,
        failureLocation: String? = null
    ) {
        eventPublisher.publish(
            MerkleVerificationFailedEvent(
                artifactId = artifactId,
                rootHash = rootHash,
                reason = reason,
                failureLocation = failureLocation
            )
        )
    }
}