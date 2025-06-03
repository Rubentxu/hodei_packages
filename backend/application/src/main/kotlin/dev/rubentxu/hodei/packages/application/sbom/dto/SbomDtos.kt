package dev.rubentxu.hodei.packages.application.sbom.dto

import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomRelationship
import java.time.Instant

/**
 * DTOs para la capa de aplicación relacionados con SBOM.
 * Estos objetos se utilizan para transferir datos entre la capa de aplicación y las interfaces externas.
 */

/**
 * DTO para la entrada de creación de un documento SBOM.
 */
data class CreateSbomRequest(
    val artifactId: String,
    val format: String,
    val version: String? = null,
    val components: List<SbomComponentDto> = emptyList(),
    val relationships: List<SbomRelationshipDto> = emptyList()
) {
    fun toSbomDocument(): SbomDocument {
        return SbomDocument(
            artifactId = artifactId,
            format = SbomFormat.fromString(format),
            version = version,
            components = components.map { it.toDomainComponent() },
            relationships = relationships.map { it.toDomainRelationship() }
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
    val supplier: String? = null,
    val description: String? = null,
    val license: String? = null,
    val purl: String? = null,
    val cpe: String? = null
) {
    fun toDomainComponent(): SbomComponent {
        return SbomComponent(
            name = name,
            version = version,
            type = type,
            supplier = supplier,
            description = description,
            license = license,
            purl = purl,
            cpe = cpe
        )
    }
    
    companion object {
        fun fromDomainComponent(component: SbomComponent): SbomComponentDto {
            return SbomComponentDto(
                name = component.name,
                version = component.version,
                type = component.type,
                supplier = component.supplier,
                description = component.description,
                license = component.license,
                purl = component.purl,
                cpe = component.cpe
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
    val type: String
) {
    fun toDomainRelationship(): SbomRelationship {
        return SbomRelationship(
            fromComponentId = fromComponentId,
            toComponentId = toComponentId,
            type = type
        )
    }
    
    companion object {
        fun fromDomainRelationship(relationship: SbomRelationship): SbomRelationshipDto {
            return SbomRelationshipDto(
                fromComponentId = relationship.fromComponentId,
                toComponentId = relationship.toComponentId,
                type = relationship.type
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
    val relationships: List<SbomRelationshipDto>
) {
    companion object {
        fun fromDomainDocument(document: SbomDocument): SbomDocumentResponse {
            return SbomDocumentResponse(
                id = document.id,
                artifactId = document.artifactId,
                format = document.format.name,
                version = document.version,
                creationTime = document.creationTime,
                components = document.components.map { SbomComponentDto.fromDomainComponent(it) },
                relationships = document.relationships.map { SbomRelationshipDto.fromDomainRelationship(it) }
            )
        }
    }
}

/**
 * DTO para una solicitud de análisis de SBOM.
 */
data class SbomAnalysisRequest(
    val sbomId: String,
    val analysisType: String = "vulnerability" // vulnerable, license, dependency
)

/**
 * DTO para la respuesta de un análisis de SBOM.
 */
data class SbomAnalysisResponse(
    val sbomId: String,
    val artifactId: String,
    val analysisType: String,
    val analysisDate: Instant,
    val issuesFound: Int,
    val severity: String?, // null, low, medium, high, critical
    val details: Map<String, Any>
)
