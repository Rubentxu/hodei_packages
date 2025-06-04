package dev.rubentxu.hodei.packages.application.sbom

import dev.rubentxu.hodei.packages.application.sbom.dto.CreateSbomRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisRequest
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomAnalysisResponse
import dev.rubentxu.hodei.packages.application.sbom.dto.SbomDocumentResponse

/**
 * Interfaz del servicio de aplicación para operaciones SBOM.
 * Define las operaciones disponibles para la gestión de documentos SBOM.
 */
interface SbomService {
    /**
     * Crea un nuevo documento SBOM a partir de la información proporcionada.
     *
     * @param request Petición con la información necesaria para crear el documento SBOM
     * @return Respuesta con el documento SBOM creado
     */
    suspend fun createSbom(request: CreateSbomRequest): Result<SbomDocumentResponse>

    /**
     * Genera un documento SBOM a partir del contenido de un artefacto.
     * Este método analiza el contenido del artefacto y extrae automáticamente
     * la información de componentes y dependencias.
     *
     * @param artifactId ID del artefacto para el que generar el SBOM
     * @param format Formato del SBOM (CYCLONE_DX, SPDX)
     * @return Respuesta con el documento SBOM generado
     */
    suspend fun generateSbomFromArtifact(
        artifactId: String,
        format: String,
    ): Result<SbomDocumentResponse>

    /**
     * Obtiene un documento SBOM por su ID.
     *
     * @param sbomId ID del documento SBOM a recuperar
     * @return Respuesta con el documento SBOM solicitado
     */
    suspend fun getSbomById(sbomId: String): Result<SbomDocumentResponse>

    /**
     * Obtiene todos los documentos SBOM asociados a un artefacto.
     *
     * @param artifactId ID del artefacto
     * @return Lista de documentos SBOM asociados al artefacto
     */
    suspend fun getSbomsByArtifactId(artifactId: String): Result<List<SbomDocumentResponse>>

    /**
     * Obtiene la versión más reciente del documento SBOM para un artefacto.
     *
     * @param artifactId ID del artefacto
     * @return Último documento SBOM del artefacto
     */
    suspend fun getLatestSbomByArtifactId(artifactId: String): Result<SbomDocumentResponse>

    /**
     * Busca documentos SBOM que contengan un componente específico.
     *
     * @param componentName Nombre del componente a buscar
     * @param componentVersion Versión opcional del componente
     * @return Lista de documentos SBOM que contienen el componente especificado
     */
    suspend fun findSbomsByComponent(
        componentName: String,
        componentVersion: String? = null,
    ): Result<List<SbomDocumentResponse>>

    /**
     * Realiza un análisis de un documento SBOM (vulnerabilidades, licencias, etc).
     *
     * @param request Detalles del análisis a realizar
     * @return Resultado del análisis
     */
    suspend fun analyzeSbom(request: SbomAnalysisRequest): Result<SbomAnalysisResponse>
}
