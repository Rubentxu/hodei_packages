package dev.rubentxu.hodei.packages.domain.ports.sbom

import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat

/**
 * Port for generating SBOM documents.
 */
interface SbomGeneratorPort {
    /**
     * Generates an SBOM document from artifact bytes.
     *
     * @param artifactBytes The byte content of the artifact.
     * @param format The desired SBOM format.
     * @return A Result containing the SbomDocument on success, or a Throwable on failure.
     */
    suspend fun generate(artifactBytes: ByteArray, format: SbomFormat): Result<SbomDocument>
}