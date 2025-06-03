package dev.rubentxu.hodei.packages.domain.model.sbom

import java.util.UUID

/**
 * Representa un componente dentro de un documento SBOM (Software Bill of Materials).
 * Cada componente es un elemento de software (biblioteca, framework, herramienta) que forma
 * parte de un artefacto más grande y se identifica por su nombre y versión.
 *
 * @param name Nombre del componente (ej. "org.apache.commons:commons-lang3")
 * @param version Versión del componente (ej. "3.12.0")
 * @param type Tipo de componente (ej. "library", "framework", "application")
 * @param licenses Lista de identificadores de licencias asociadas al componente (opcional)
 * @param description Descripción del componente (opcional)
 */
data class SbomComponent(
    val name: String,
    val version: String,
    val type: String,
    val licenses: List<String> = emptyList(),
    val description: String? = null
) {
    val id: String

    init {
        require(name.isNotBlank()) { "Component name cannot be blank" }
        require(version.isNotBlank()) { "Component version cannot be blank" }
        require(type.isNotBlank()) { "Component type cannot be blank" }
        
        id = generateComponentId(name, version)
    }

    /**
     * Genera un identificador único y consistente para el componente basado en su nombre y versión.
     * Esto garantiza que el mismo componente (nombre+versión) siempre tenga el mismo ID.
     */
    private fun generateComponentId(name: String, version: String): String {
        return UUID.nameUUIDFromBytes("${name}:${version}".toByteArray()).toString()
    }
}
