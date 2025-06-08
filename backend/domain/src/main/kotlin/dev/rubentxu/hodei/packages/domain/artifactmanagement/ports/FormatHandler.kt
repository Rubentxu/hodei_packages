package dev.rubentxu.hodei.packages.domain.artifactmanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId

/**
 * Defines the contract for a handler specific to an artifact format (e.g., Maven, NPM, Docker).
 * Implementations of this interface are responsible for parsing format-specific metadata,
 * deriving artifact coordinates, and potentially generating format-specific descriptor files.
 */
interface FormatHandler {
    /**
     * Extrae las coordenadas del artefacto utilizando un enfoque híbrido.
     * 1. Intenta extraer del contenido primero (descomprimiendo si es necesario)
     * 2. Si falla, intenta inferir del nombre del archivo
     * 3. Si falla, usa los metadatos proporcionados externamente
     *
     * @param filename El nombre original del archivo subido.
     * @param content El array de bytes del contenido del artefacto.
     * @param providedMetadata Metadatos opcionales proporcionados por el usuario o sistema.
     * @return Un [Result] que contiene [ArtifactCoordinates] con su fuente.
     */
    fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>? = null
    ): Result<Pair<ArtifactCoordinates, MetadataSource>>

    /**
     * Analiza metadatos del artefacto utilizando un enfoque híbrido con varios fallbacks.
     * 1. Primero intenta extraer del contenido descomprimiendo el artefacto
     * 2. Si falla, intenta inferir del nombre del archivo
     * 3. Si aún falta información, utiliza los metadatos proporcionados
     * 4. Registra la fuente de cada campo para validación
     *
     * @param filename El nombre original del archivo subido.
     * @param content El array de bytes del contenido del artefacto.
     * @param providedMetadata Metadatos opcionales proporcionados externamente.
     * @param artifactId ID del artefacto, requerido para crear los metadatos.
     * @param userId ID del usuario que sube el artefacto.
     * @return Un [Result] con [ArtifactMetadataWithSources] que contiene los metadatos y sus fuentes.
     */
    fun extractMetadataWithSources(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>? = null,
        artifactId: ArtifactId,
        userId: UserId
    ): Result<ArtifactMetadataWithSources>

    /**
     * Determina el tipo de empaquetado (ej. "jar", "tar.gz", "whl") según el formato.
     * También usa un enfoque híbrido, priorizando el contenido sobre el nombre de archivo.
     *
     * @param filename El nombre original del archivo subido.
     * @param content El array de bytes del contenido del artefacto.
     * @return Un [Result] que contiene el tipo de empaquetado con su fuente.
     */
    fun determinePackagingType(filename: String, content: ByteArray): Result<Pair<String, MetadataSource>>

    /**
     * Genera el contenido de un archivo descriptor específico del formato (ej. POM.xml, package.json)
     * basado en los datos del [Artifact] proporcionado.
     *
     * @param artifact El [Artifact] para el cual generar el descriptor.
     * @return Un [Result] que contiene el contenido del descriptor como [String] o un error.
     */
    fun generateDescriptor(artifact: Artifact): Result<String>

    /**
     * Valida los metadatos proporcionados por el usuario contra los extraídos del contenido.
     * Identifica inconsistencias y determina si deben rechazarse.
     *
     * @param extracted Metadatos extraídos del contenido.
     * @param provided Metadatos proporcionados por el usuario.
     * @return Un [Result] que contiene `true` si válidos, error si hay inconsistencias.
     */
    fun validateMetadataConsistency(
        extracted: ArtifactMetadataWithSources,
        provided: Map<String, String>
    ): Result<Boolean> {
        // Implementación por defecto: acepta sin validación
        return Result.success(true)
    }

    /**
     * Extrae dependencias específicas del formato desde el contenido del artefacto.
     * Este método es importante para construir grafos de dependencias.
     *
     * @param content El array de bytes del contenido del artefacto.
     * @return Un [Result] que contiene una lista de dependencias con sus versiones.
     */
    fun extractDependencies(
        content: ByteArray
    ): Result<List<ArtifactDependency>>
}