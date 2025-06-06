package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

import java.util.*

/**
 * Represents a component within an SBOM (Software Bill of Materials).
 * Each component is a software element (library, framework, tool)
 * identified by its group, name, and version.
 *
 * @param group Organizational entity or namespace for the component (e.g., "org.apache.commons", "@angular"). Optional.
 * @param name Name of the component (e.g., "commons-lang3", "core").
 * @param version Version of the component (e.g., "3.12.0", "14.0.0").
 * @param type Type of component (e.g., "library", "framework", "application", "file").
 * @param scope The intended use or scope of the component (e.g., required, optional, excluded).
 * @param licenses List of license identifiers or expressions associated with the component.
 * @param description Description of the component.
 * @param supplier Supplier or vendor of the component (e.g., "Apache Software Foundation").
 * @param purl Package URL (PURL) for precise identification.
 * @param cpe Common Platform Enumeration (CPE) identifier.
 * @param swidTagId Software Identification (SWID) Tag ID.
 * @param copyright Copyright information for the component.
 * @param hashes Map of cryptographic hashes for the component (algorithm to value).
 * @param externalReferences Links to external resources related to the component.
 * @param properties Additional custom properties for the component.
 * @param components List of sub-components, allowing for a hierarchical structure.
 */
data class SbomComponent(
    val group: String? = null,
    val name: String,
    val version: String,
    val type: String,
    val scope: ComponentScope? = null,
    val licenses: List<String> = emptyList(),
    val description: String? = null,
    val supplier: String? = null,
    val purl: String? = null,
    val cpe: String? = null,
    val swidTagId: String? = null,
    val copyright: String? = null,
    val hashes: Map<String, String> = emptyMap(),
    val externalReferences: List<ExternalReference>? = null,
    val properties: Map<String, String>? = null,
    val components: List<SbomComponent>? = null // For nested components
) {
    /**
     * Unique identifier for this component within the SBOM, often referred to as "bom-ref" in CycloneDX
     * or used for SPDXID generation contextually.
     * It's generated to be consistent for the same component attributes.
     */
    val id: String

    init {
        require(name.isNotBlank()) { "Component name cannot be blank" }
        require(version.isNotBlank()) { "Component version cannot be blank" }
        require(type.isNotBlank()) { "Component type cannot be blank" }
        group?.let { require(it.isNotBlank()) { "Component group, if provided, cannot be blank" } }

        id = generateComponentId(group, name, version)
    }

    /**
     * Generates a unique and consistent identifier for the component based on its group, name, and version.
     */
    private fun generateComponentId(group: String?, name: String, version: String): String {
        val identifierBase = listOfNotNull(group, name, version).joinToString(":")
        return UUID.nameUUIDFromBytes(identifierBase.toByteArray()).toString()
    }
}

/**
 * Defines the scope or intended use of an SbomComponent.
 * This helps in understanding the role of a component, especially in dependency contexts.
 * For example, whether a component is a direct dependency, a transitive one,
 * or part of the test/build environment.
 */
enum class ComponentScope {
    /**
     * The component is required for the software to function correctly.
     * Corresponds to CycloneDX 'required'.
     */
    REQUIRED,

    /**
     * The component is optional and the software can function without it,
     * though perhaps with reduced functionality.
     * Corresponds to CycloneDX 'optional'.
     */
    OPTIONAL,

    /**
     * The component is not part of the delivered software but might have been
     * used during the build or development process (e.g., a test library).
     * Corresponds to CycloneDX 'excluded'.
     */
    EXCLUDED,

    /**
     * The component is used in a specific context, such as testing or development,
     * but not in the production runtime.
     * This is a more general scope that can be used for various contexts.
     */
    RUNTIME,
}

// The ExternalReference class would be the same as defined previously for SbomDocument.
// data class ExternalReference(val type: String, val url: String, val comment: String? = null)