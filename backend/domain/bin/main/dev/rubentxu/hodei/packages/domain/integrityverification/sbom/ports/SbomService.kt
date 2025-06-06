package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports

/**
 * Servicio de aplicación para la gestión de SBOMs (Software Bill of Materials).
 * Define los casos de uso principales para trabajar con SBOMs desde la capa de aplicación,
 * siguiendo los principios de la arquitectura hexagonal.
 */
interface SbomService {
    /**
     * Guarda un documento SBOM en el sistema.
     * @param sbomDocument El documento SBOM a guardar
     * @return Resultado encapsulando el documento guardado o un error
     */
    suspend fun saveSbom(sbomDocument: SbomDocument): Result<SbomDocument>

    /**
     * Recupera un documento SBOM por su identificador único.
     * @param id El identificador único del SBOM
     * @return Resultado encapsulando el documento encontrado (o null si no existe) o un error
     */
    suspend fun findById(id: String): Result<SbomDocument?>

    /**
     * Recupera todos los SBOMs asociados a un artefacto.
     * @param artifactId El identificador del artefacto
     * @return Resultado encapsulando la lista de SBOMs encontrados o un error
     */
    suspend fun findByArtifactId(artifactId: String): Result<List<SbomDocument>>

    /**
     * Recupera el SBOM más reciente para un artefacto y formato específicos.
     * @param artifactId El identificador del artefacto
     * @param format El formato del SBOM (opcional)
     * @return Resultado encapsulando el SBOM más reciente (o null si no existe) o un error
     */
    suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat? = null): Result<SbomDocument?>

    /**
     * Busca SBOMs por componente.
     * @param componentName Nombre del componente
     * @param componentVersion Versión del componente (opcional)
     * @return Resultado encapsulando la lista de SBOMs donde aparece el componente o un error
     */
    suspend fun findByComponent(componentName: String, componentVersion: String? = null): Result<List<SbomDocument>>
}