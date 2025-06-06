package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleGraph
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleNode
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature

/**
 * Application service for managing Merkle graphs.
 * Orchestrates the creation, validation, signing, and querying of Merkle graphs associated with artifacts,
 * following the principles of hexagonal architecture and Clean Code.
 */
interface MerkleGraphService {
    /**
     * Creates a new Merkle graph for a given artifact.
     * @param artifactId Identifier of the artifact
     * @param rootNode The root node of the Merkle tree
     * @return Result encapsulating the created graph or an error
     */
    suspend fun createGraph(artifactId: String, rootNode: MerkleNode): Result<MerkleGraph>

    /**
     * Adds a digital signature to an existing Merkle graph.
     * @param artifactId Identifier of the artifact
     * @param signature Signature to add
     * @return Result encapsulating the updated graph or an error
     */
    suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph>

    /**
     * Verifies the structural and cryptographic integrity of a Merkle graph.
     * @param artifactId Identifier of the artifact
     * @return Result encapsulating true if the graph is valid, false if not, or an error
     */
    suspend fun verifyGraph(artifactId: String): Result<Boolean>

    /**
     * Retrieves a Merkle graph by the artifact ID.
     * @param artifactId Identifier of the artifact
     * @return Result encapsulating the found graph (or null if it does not exist) or an error
     */
    suspend fun findByArtifactId(artifactId: String): Result<MerkleGraph?>

    /**
     * Retrieves a Merkle graph by its root hash.
     * @param rootHash Root hash of the graph
     * @return Result encapsulating the found graph (or null if it does not exist) or an error
     */
    suspend fun findByRootHash(rootHash: ContentHash): Result<MerkleGraph?>

    /**
     * Lists all Merkle graphs signed by a specific keyId.
     * @param keyId Identifier of the key (signer)
     * @return Result encapsulating the list of graphs or an error
     */
    suspend fun findBySignatureKeyId(keyId: String): Result<List<MerkleGraph>>
}