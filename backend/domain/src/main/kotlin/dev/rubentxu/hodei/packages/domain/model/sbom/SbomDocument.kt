package dev.rubentxu.hodei.packages.domain.model.sbom

import java.time.Instant
import java.util.UUID

/**
 * Representa un documento SBOM (Software Bill of Materials) completo.
 * Un SBOM es un inventario formal y estructurado de componentes, bibliotecas y módulos que componen
 * un artefacto de software, junto con sus relaciones de dependencia.
 *
 * @param artifactId ID del artefacto al que pertenece este SBOM
 * @param format Formato del documento SBOM (CycloneDX, SPDX, etc.)
 * @param components Lista de componentes incluidos en el SBOM
 * @param relationships Lista de relaciones entre los componentes (opcional)
 * @param version Versión del formato SBOM utilizado (opcional, por defecto "1.0")
 * @param creationTime Momento de creación del documento (opcional, por defecto el momento actual)
 */
data class SbomDocument(
    val artifactId: String,
    val format: SbomFormat,
    val components: List<SbomComponent>,
    val relationships: List<SbomRelationship> = emptyList(),
    val version: String = "1.0",
    val creationTime: Instant = Instant.now()
) {
    val id: String

    init {
        require(artifactId.isNotBlank()) { "ArtifactId cannot be blank" }
        require(components.isNotEmpty()) { "Components list cannot be empty" }
        
        id = generateDocumentId(artifactId, creationTime)
    }

    /**
     * Genera un identificador único para el documento SBOM basado en su artifactId y momento de creación.
     */
    private fun generateDocumentId(artifactId: String, creationTime: Instant): String {
        return UUID.nameUUIDFromBytes("$artifactId:${creationTime.toEpochMilli()}".toByteArray()).toString()
    }
}
