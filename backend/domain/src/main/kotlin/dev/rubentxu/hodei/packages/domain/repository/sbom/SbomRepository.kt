package dev.rubentxu.hodei.packages.domain.repository.sbom

import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat

/**
 * Puerto para el acceso a documentos SBOM.
 * Define las operaciones que cualquier implementación de repositorio SBOM debe proporcionar.
 * Siguiendo los principios de la arquitectura hexagonal, esta interfaz actúa como
 * un puerto en el modelo de dominio que será implementado por adaptadores
 * en la capa de infraestructura.
 */
interface SbomRepository {
    /**
     * Guarda un documento SBOM en el repositorio.
     * @param sbomDocument El documento SBOM a guardar
     * @return Resultado encapsulando el documento guardado o un error
     */
    suspend fun save(sbomDocument: SbomDocument): Result<SbomDocument>

    /**
     * Recupera un documento SBOM por su identificador único.
     * @param id El identificador único del documento SBOM
     * @return Resultado encapsulando el documento encontrado (o null si no existe) o un error
     */
    suspend fun findById(id: String): Result<SbomDocument?>

    /**
     * Recupera todos los documentos SBOM asociados a un artefacto específico.
     * @param artifactId El identificador del artefacto
     * @return Resultado encapsulando la lista de documentos SBOM encontrados o un error
     */
    suspend fun findByArtifactId(artifactId: String): Result<List<SbomDocument>>

    /**
     * Recupera el documento SBOM más reciente para un artefacto y formato específicos.
     * @param artifactId El identificador del artefacto
     * @param format El formato del SBOM (opcional)
     * @return Resultado encapsulando el documento SBOM más reciente (o null si no existe) o un error
     */
    suspend fun findLatestByArtifactId(artifactId: String, format: SbomFormat? = null): Result<SbomDocument?>

    /**
     * Busca artefactos que contengan un componente específico.
     * @param componentName Nombre del componente a buscar
     * @param componentVersion Versión del componente (opcional)
     * @return Resultado encapsulando la lista de documentos SBOM donde aparece el componente o un error
     */
    suspend fun findByComponent(componentName: String, componentVersion: String? = null): Result<List<SbomDocument>>
}
