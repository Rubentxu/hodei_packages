package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

import java.time.Instant
import java.util.*

/**
 * Represents a complete SBOM (Software Bill of Materials) document.
 * An SBOM is a formal, structured inventory of components, libraries, and modules
 * that make up a piece of software, along with their dependency relationships.
 *
 * @param artifactId ID of the artifact this SBOM belongs to (links to Artifact in your registry).
 * @param format Format of the SBOM document (CycloneDX, SPDX, etc.).
 * @param specVersion Version of the SBOM specification used (e.g., "1.4" for CycloneDX, "2.3" for SPDX).
 * @param components List of components included in the SBOM.
 * @param relationships List of relationships between components (optional).
 * @param creationTime Timestamp of when the document was created (defaults to now).
 * @param tools Information about the tools used to generate this SBOM (optional).
 * @param authors Information about the authors or creators of this SBOM (optional).
 * @param serialNumber A version or serial number for this specific SBOM document instance (optional).
 * @param documentName A human-readable name for the SBOM document (e.g., "SBOM for MyProduct v1.2").
 * @param documentNamespace A unique URI for the SBOM document, for interoperability (e.g., an SPDX document namespace).
 * @param describesComponentRef Reference (e.g., SbomComponent.id or bom-ref) to the primary component this SBOM describes.
 * @param externalReferences List of external references related to the SBOM or the described component.
 * @param dataLicense The license under which the SBOM document itself is distributed (e.g., "CC0-1.0").
 */
data class SbomDocument(
    val artifactId: String,
    val format: SbomFormat,
    val specVersion: String,
    val components: List<SbomComponent>,
    val relationships: List<SbomRelationship> = emptyList(),
    val creationTime: Instant = Instant.now(),
    val tools: List<ToolInformation>? = null,
    val authors: List<ContactInformation>? = null,
    val serialNumber: String? = null,
    // New/Enhanced fields
    val documentName: String? = null,
    val documentNamespace: String? = null,
    val describesComponentRef: String? = null, // Refers to an SbomComponent.id within the 'components' list
    val externalReferences: List<ExternalReference>? = null,
    val dataLicense: String? = null // e.g., "CC0-1.0" for the SBOM data itself
) {
    val id: String

    init {
        require(artifactId.isNotBlank()) { "ArtifactId cannot be blank" }
        require(specVersion.isNotBlank()) { "SpecVersion cannot be blank" }
        require(components.isNotEmpty()) { "Components list cannot be empty for a typical software SBOM" }
        if (describesComponentRef != null) {
            require(components.any { it.id == describesComponentRef }) {
                "If describesComponentRef is provided, it must match the id of a component in the components list."
            }
        }

        // Consider if documentNamespace should be part of the ID generation for global uniqueness
        id = generateDocumentId(artifactId, creationTime, serialNumber, documentNamespace)
    }

    /**
     * Generates a unique identifier for the SBOM document.
     * The uniqueness can be scoped to your system or made more globally unique
     * by incorporating elements like documentNamespace if available.
     */
    private fun generateDocumentId(
        artifactId: String,
        creationTime: Instant,
        serialNumber: String?,
        documentNamespace: String?
    ): String {
        val baseString = StringBuilder()
        baseString.append(documentNamespace ?: "urn:uuid:${UUID.randomUUID()}") // Prefer namespace if available
        baseString.append(":$artifactId:${creationTime.toEpochMilli()}")
        serialNumber?.let { baseString.append(":$it") }

        // If documentNamespace is a URN or URL that's already unique,
        // you might use it more directly or combine it differently.
        // For now, using it as part of the input to a UUID generation.
        return UUID.nameUUIDFromBytes(baseString.toString().toByteArray()).toString()
    }
}

/**
 * Represents an external reference with a type, URL, and optional comment.
 * Types are often defined by SBOM specifications (e.g., "vcs", "website", "issue-tracker").
 *
 * @param type The type of external reference (e.g., "vcs", "website", "issue-tracker", "documentation").
 * @param url The URL of the external reference.
 * @param comment An optional comment about the external reference.
 */
data class ExternalReference(
    val type: String,
    val url: String,
    val comment: String? = null
)

// Ensure ToolInformation and ContactInformation classes are defined as in your previous context.
// data class ToolInformation(...)
// data class ContactInformation(...)