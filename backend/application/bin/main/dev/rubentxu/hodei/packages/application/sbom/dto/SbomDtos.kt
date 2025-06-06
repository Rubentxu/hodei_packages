package dev.rubentxu.hodei.packages.application.sbom.dto

import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomRelationship
import java.time.Instant

/**
 * DTO para la entrada de creación de un documento SBOM.
 */
data class CreateSbomRequest(
    val artifactId: String,
    val format: String, // This is the one that SbomFormat.fromString uses
    val version: String? = null,
    val components: List<SbomComponentDto> = emptyList(),
    val relationships: List<SbomRelationshipDto> = emptyList(),
) {
    fun toSbomDocument(): SbomDocument {
        return SbomDocument(
            artifactId = artifactId,
            // The error was: actual type is 'SbomFormat?', but 'SbomFormat' was expected.
            // If SbomFormat.fromString(format) returns SbomFormat?, then this line is problematic
            // because SbomDocument expects SbomFormat.
            // SbomFormat.fromString now returns SbomFormat? so we must handle null.
            format = SbomFormat.fromString(format) ?: throw IllegalArgumentException("Invalid or unsupported SBOM format string: $format"),
            version = version ?: "1.0",
            components = components.map { it.toDomainComponent() },
            relationships = relationships.map { it.toDomainRelationship() },
        )
    }
}

/**
 * DTO para la representación de un componente SBOM.
 */
data class SbomComponentDto(
    val name: String,
    val version: String,
    val type: String = "library",
    val description: String? = null,
    val licenses: List<String> = emptyList(),
) {
    fun toDomainComponent(): SbomComponent {
        return SbomComponent(
            name = name,
            version = version,
            type = type,
            description = description,
            licenses = licenses,
        )
    }

    companion object {
        fun fromDomainComponent(component: SbomComponent): SbomComponentDto {
            return SbomComponentDto(
                name = component.name,
                version = component.version,
                type = component.type,
                description = component.description,
                licenses = component.licenses,
            )
        }
    }
}

/**
 * DTO para la representación de una relación entre componentes SBOM.
 */
data class SbomRelationshipDto(
    val fromComponentId: String,
    val toComponentId: String,
    val type: String,
) {
    fun toDomainRelationship(): SbomRelationship {
        return SbomRelationship(
            fromComponentId = fromComponentId,
            toComponentId = toComponentId,
            type = type,
        )
    }

    companion object {
        fun fromDomainRelationship(relationship: SbomRelationship): SbomRelationshipDto {
            return SbomRelationshipDto(
                fromComponentId = relationship.fromComponentId,
                toComponentId = relationship.toComponentId,
                type = relationship.type,
            )
        }
    }
}

/**
 * DTO para la respuesta de un documento SBOM.
 */
data class SbomDocumentResponse(
    val id: String,
    val artifactId: String,
    val format: String,
    val version: String?,
    val creationTime: Instant,
    val components: List<SbomComponentDto>,
    val relationships: List<SbomRelationshipDto>,
) {
    companion object {
        fun fromDomainDocument(document: SbomDocument): SbomDocumentResponse {
            return SbomDocumentResponse(
                id = document.id,
                artifactId = document.artifactId,
                format = document.format.name, // Uses the name of the enum, which is String
                version = document.version,
                creationTime = document.creationTime,
                components = document.components.map { SbomComponentDto.fromDomainComponent(it) },
                relationships = document.relationships.map { SbomRelationshipDto.fromDomainRelationship(it) },
            )
        }
    }
}

// SbomAnalysisRequest and SbomAnalysisResponse are now defined in their own dedicated files
// (SbomAnalysisRequest.kt and SbomAnalysisResponse.kt respectively)
// to prevent redeclaration errors and improve organization.
// SbomAnalysisResponse.kt also contains VulnerabilityDto and LicenseComplianceDto.
