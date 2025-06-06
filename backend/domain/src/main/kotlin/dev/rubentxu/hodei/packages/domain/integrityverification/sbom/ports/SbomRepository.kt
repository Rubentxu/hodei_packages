package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat

/**
 * Port for accessing SBOM documents.
 * Defines the operations that any SBOM repository implementation must provide.
 * Following the principles of hexagonal architecture, this interface acts as
 * a port in the domain model that will be implemented by adapters
 * in the infrastructure layer.
 */
interface SbomRepository {
    /**
     * Saves an SBOM document to the repository.
     * @param sbomDocument The SBOM document to save
     * @return Result encapsulating the saved document or an error
     */
    suspend fun save(sbomDocument: SbomDocument): Result<SbomDocument>

    /**
     * Retrieves an SBOM document by its unique identifier.
     * @param id The unique identifier of the SBOM document
     * @return Result encapsulating the found document (or null if it does not exist) or an error
     */
    suspend fun findById(id: String): Result<SbomDocument?>

    /**
     * Retrieves all SBOM documents associated with a specific artifact.
     * @param artifactId The identifier of the artifact
     * @return Result encapsulating the list of SBOM documents found or an error
     */
    suspend fun findByArtifactId(artifactId: String): Result<List<SbomDocument>>

    /**
     * Retrieves the most recent SBOM document for a specific artifact and format.
     * @param artifactId The identifier of the artifact
     * @param format The SBOM format (optional)
     * @return Result encapsulating the most recent SBOM document (or null if it does not exist) or an error
     */
    suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat? = null): Result<SbomDocument?>

    /**
     * Searches for artifacts that contain a specific component.
     * @param componentName Name of the component to search for
     * @param componentVersion Component version (optional)
     * @return Result encapsulating the list of SBOM documents where the component appears or an error
     */
    suspend fun findByComponent(componentName: String, componentVersion: String? = null): Result<List<SbomDocument>>
}