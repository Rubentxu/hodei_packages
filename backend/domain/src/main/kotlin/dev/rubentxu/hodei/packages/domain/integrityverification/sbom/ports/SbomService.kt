package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat

/**
 * Application service for managing Software Bill of Materials (SBOMs).
 * Defines the main use cases for working with SBOMs from the application layer,
 * following the principles of hexagonal architecture.
 */
interface SbomService {
    /**
     * Saves an SBOM document in the system.
     * @param sbomDocument The SBOM document to save.
     * @return Result encapsulating the saved document or an error.
     */
    suspend fun saveSbom(sbomDocument: SbomDocument): Result<SbomDocument>

    /**
     * Retrieves an SBOM document by its unique identifier.
     * @param id The unique identifier of the SBOM.
     * @return Result encapsulating the found document (or null if it does not exist) or an error.
     */
    suspend fun findById(id: String): Result<SbomDocument?>

    /**
     * Retrieves all SBOMs associated with an artifact.
     * @param artifactId The identifier of the artifact.
     * @return Result encapsulating the list of SBOMs found or an error.
     */
    suspend fun findByArtifactId(artifactId: String): Result<List<SbomDocument>>

    /**
     * Retrieves the latest SBOM for a specific artifact and format.
     * @param artifactId The identifier of the artifact.
     * @param format The format of the SBOM (optional).
     * @return Result encapsulating the latest SBOM (or null if it does not exist) or an error.
     */
    suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat? = null): Result<SbomDocument?>

    /**
     * Searches SBOMs by component.
     * @param componentName The name of the component.
     * @param componentVersion The version of the component (optional).
     * @return Result encapsulating the list of SBOMs where the component appears or an error.
     */
    suspend fun findByComponent(componentName: String, componentVersion: String? = null): Result<List<SbomDocument>>
}
