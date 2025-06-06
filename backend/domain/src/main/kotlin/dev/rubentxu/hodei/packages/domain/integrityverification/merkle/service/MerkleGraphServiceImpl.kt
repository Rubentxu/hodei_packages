package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.service

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleGraph
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNode
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.MerkleGraphService
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports.MerkleGraphRepository

/**
 * Implementation of [MerkleGraphService] that orchestrates the business logic
 * and delegates to the Merkle graph repository.
 */
class MerkleGraphServiceImpl(
    private val repository: MerkleGraphRepository
) : MerkleGraphService {
    override suspend fun createGraph(artifactId: String, rootNode: MerkleNode): Result<MerkleGraph> {
        val graph = MerkleGraph(artifactId, rootNode, emptyList())
        return repository.save(graph)
    }

    override suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph> {
        return repository.addSignature(artifactId, signature)
    }

    override suspend fun verifyGraph(artifactId: String): Result<Boolean> {
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