package dev.rubentxu.hodei.packages.domain.integrityverification.vex.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.integrityverification.vex.model.VexDocument


/**
 * Port for accessing VEX documents.
 * Defines the operations for storing and retrieving VEX documents.
 */
interface VexRepository {
    /**
     * Saves a VEX document.
     * @param vex The VEX document to save
     * @return Result with the saved VEX document or an error
     */
    suspend fun save(vex: VexDocument): Result<VexDocument>

    /**
     * Finds VEX documents by artifact ID.
     * @param artifactId The ID of the artifact
     * @return Result with the list of VEX documents or an error
     */
    suspend fun findByArtifactId(artifactId: ArtifactId): Result<List<VexDocument>>

    /**
     * Finds VEX documents by CVE ID.
     * @param cveId The CVE ID to search for
     * @return Result with the list of VEX documents or an error
     */
    suspend fun findByCve(cveId: String): Result<List<VexDocument>>
}