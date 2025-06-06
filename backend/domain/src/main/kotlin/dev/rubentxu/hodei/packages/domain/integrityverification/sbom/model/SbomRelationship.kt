package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

/**
 * Representa una relación entre dos componentes dentro de un documento SBOM.
 * Por ejemplo, un componente puede "depender de" otro componente.
 *
 * @param fromComponentId ID del componente de origen
 * @param toComponentId ID del componente de destino
 * @param type Tipo de relación (ej. "DEPENDS_ON", "CONTAINS", etc.)
 */
data class SbomRelationship(
    val fromComponentId: String,
    val toComponentId: String,
    val type: String
) {
    init {
        require(fromComponentId.isNotBlank()) { "Source component ID cannot be blank" }
        require(toComponentId.isNotBlank()) { "Target component ID cannot be blank" }
        require(type.isNotBlank()) { "Relationship type cannot be blank" }
    }
}
