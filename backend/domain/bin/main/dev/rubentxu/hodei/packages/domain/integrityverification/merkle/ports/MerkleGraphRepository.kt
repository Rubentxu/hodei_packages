package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.ports

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.MerkleGraph
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature

/**
 * Port for accessing Merkle graphs.
 * Defines the operations for storing, retrieving, and verifying Merkle graphs
 * that represent the cryptographic verification structure of artifacts.
 *
 * Following the principles of hexagonal architecture, this interface acts as
 * a port in the domain model that will be implemented by adapters
 * in the infrastructure layer.
 */
interface MerkleGraphRepository {
    /**
     * Saves a Merkle graph to the repository.
     *
     * @param graph The Merkle graph to save
     * @return Result encapsulating the saved graph or an error
     */
    suspend fun save(graph: MerkleGraph): Result<MerkleGraph>

    /**
     * Retrieves a Merkle graph by the ID of the associated artifact.
     *
     * @param artifactId The ID of the artifact
     * @return Result encapsulating the found graph (or null if it does not exist) or an error
     */
    suspend fun findByArtifactId(artifactId: String): Result<MerkleGraph?>

    /**
     * Retrieves a Merkle graph by its root hash.
     *
     * @param rootHash The root hash that uniquely identifies a state of the artifact
     * @return Result encapsulating the found graph (or null if it does not exist) or an error
     */
    suspend fun findByRootHash(rootHash: ContentHash): Result<MerkleGraph?>

    /**
     * Adds a signature to an existing Merkle graph.
     *
     * @param artifactId The ID of the artifact whose graph is to be signed
     * @param signature The signature to add
     * @return Result encapsulating the updated graph or an error
     */
    suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph>

    /**
     * Verifies the structural validity of the Merkle graph, recalculating all hashes
     * from the leaves to the root and checking their consistency.
     *
     * @param artifactId The ID of the artifact to verify
     * @return Result encapsulating true if the graph is valid, false if not, or an error
     */
    suspend fun verifyGraphStructure(artifactId: String): Result<Boolean>

    /**
     * Lists all Merkle graphs associated with a given keyId (signer).
     *
     * @param keyId The ID of the key (typically the signer's email or name)
     * @return Result encapsulating the list of graphs signed by this keyId or an error
     */
    suspend fun findBySignatureKeyId(keyId: String): Result<List<MerkleGraph>>
}