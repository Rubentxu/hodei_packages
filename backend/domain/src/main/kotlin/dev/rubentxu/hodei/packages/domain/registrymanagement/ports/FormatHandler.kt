package dev.rubentxu.hodei.packages.domain.registrymanagement.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates // Necesario
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactMetadata // Ya lo tenías

/**
 * Defines the contract for a handler specific to an artifact format (e.g., Maven, NPM, Docker).
 * Implementations of this interface are responsible for parsing format-specific metadata,
 * deriving artifact coordinates, and potentially generating format-specific descriptor files.
 */
interface FormatHandler {
    /**
     * Parses the artifact coordinates from the filename and potentially the content.
     *
     * @param filename The original name of the uploaded file.
     * @param content The byte array of the artifact's content.
     * @return A [Result] containing the parsed [ArtifactCoordinates] or an error if parsing fails.
     */
    fun parseCoordinates(filename: String, content: ByteArray): Result<ArtifactCoordinates>

    /**
     * Analiza metadatos específicos del formato a partir del contenido del artefacto.
     * Estos metadatos pueden ser distintos de las propiedades principales de [Artifact] y podrían
     * almacenarse en el campo [Artifact.metadata] o en una entidad [ArtifactMetadata] separada.
     *
     * Actualmente, se ha decidido que devuelva un `Map<String, String>` para su uso directo
     * en [Artifact.metadata] por su flexibilidad.
     *
     * @param content El array de bytes del contenido del artefacto.
     * @return Un [Result] que contiene un mapa de metadatos analizados (String a String) o un error.
     *         Este mapa está pensado para el almacenamiento directo en [Artifact.metadata].
     */
    fun parseMetadata(content: ByteArray): Result<Map<String, String>>

    /**
     * Determines the packaging type (e.g., "jar", "tar.gz", "whl") from the filename or content.
     * This corresponds to [Artifact.packagingType].
     *
     * @param filename The original name of the uploaded file.
     * @param content The byte array of the artifact's content.
     * @return The packaging type string, or null if not determinable by this handler or not applicable.
     */
    fun getPackagingType(filename: String, content: ByteArray): Result<String>

    /**
     * Generates a format-specific descriptor file content (e.g., a POM.xml for Maven, a package.json for NPM)
     * based on the provided [Artifact] data.
     *
     * @param artifact The [Artifact] for which to generate the descriptor.
     * @return A [Result] containing the descriptor content as a [String] or an error if generation fails.
     */
    fun generateDescriptor(artifact: Artifact): Result<String>
}