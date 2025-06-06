package dev.rubentxu.hodei.packages.domain.model.sbom

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.Instant

class SbomDocumentTest : StringSpec({
    "SbomDocument should be created with required fields" {
        val component1 = SbomComponent(
            "package1", "1.0.0", "library",
            type = "library",
            scope = ComponentScope.RUNTIME,
            licenses = listOf("MIT"),
            description = "A test package",
            supplier = "Test Supplier",
            purl = "pkg:maven/org.example/package1@1.0.0",
            cpe = "cpe:/a:example:package1:1.0.0",
            swidTagId = "swid-1",
            copyright = "Copyright 2024",
            hashes = mapOf("SHA-256" to "abc123"),
            externalReferences = listOf(
                ExternalReference("website", "https://example.com")
            ),
            properties = mapOf("key" to "value"),
            components = emptyList()
        )
        val component2 = SbomComponent(
            "package2", "2.0.0", "library",
            type = "library",
            scope = ComponentScope.RUNTIME,
            licenses = listOf("Apache-2.0"),
            description = "Another test package",
            supplier = "Another Supplier",
            purl = "pkg:maven/org.example/package2@2.0.0",
            cpe = "cpe:/a:example:package2:2.0.0",
            swidTagId = "swid-2",
            copyright = "Copyright 2024",
            hashes = mapOf("SHA-256" to "def456"),
            externalReferences = listOf(
                ExternalReference("website", "https://another.com")
            ),
            properties = mapOf("key2" to "value2"),
            components = emptyList()
        )
        
        val doc = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            specVersion = "1.0",
            components = listOf(component1, component2)
        )
        
        doc.artifactId shouldBe "test-artifact-123"
        doc.format shouldBe SbomFormat.CYCLONE_DX
        doc.components shouldBe listOf(component1, component2)
        doc.id.shouldNotBeBlank()
        doc.specVersion shouldBe "1.0"
        doc.creationTime shouldNotBe null
        doc.relationships shouldBe emptyList()
    }
    
    "SbomDocument should support optional fields" {
        val component1 = SbomComponent(
            "package1", "1.0.0", "library",
            type = "library",  // Añadido el parámetro obligatorio 'type'
            externalReferences = listOf(ExternalReference("website", "https://example.com"))
        )
        val component2 = SbomComponent(
            "package2", "2.0.0", "library",
            type = "library",  // Añadido el parámetro obligatorio 'type'
            externalReferences = listOf(ExternalReference("website", "https://example2.com"))
        )
        
        val relationship = SbomRelationship(component1.id, component2.id, "DEPENDS_ON")
        val creationTime = Instant.now().minusSeconds(3600)
        
        val doc = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            specVersion = "2.1",
            components = listOf(component1, component2),
            relationships = listOf(relationship),
            creationTime = creationTime
        )
        
        doc.specVersion shouldBe "2.1"
        doc.creationTime shouldBe creationTime
        doc.relationships shouldContain relationship
    }
    
    "SbomDocument should validate artifactId is not blank" {
        val component = SbomComponent(
            "package1", "1.0.0", "library",
            type = "library",  // Añadido el parámetro obligatorio 'type'
            externalReferences = listOf(ExternalReference("website", "https://example.com"))
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            SbomDocument(
                artifactId = "",
                format = SbomFormat.CYCLONE_DX,
                specVersion = "1.0",
                components = listOf(component)
            )
        }
        
        exception.message shouldBe "ArtifactId cannot be blank"
    }
    
    "SbomDocument should validate components is not empty" {
        val exception = shouldThrow<IllegalArgumentException> {
            SbomDocument(
                artifactId = "test-artifact-123",
                format = SbomFormat.CYCLONE_DX,
                specVersion = "1.0",
                components = emptyList()
            )
        }
        
        exception.message shouldBe "Components list cannot be empty for a typical software SBOM"
    }
    
    "SbomDocument should generate consistent id based on artifactId and creation time" {
        val component = SbomComponent(
            "package1", "1.0.0", "library",
            type = "library",  // Añadido el parámetro obligatorio 'type'
            externalReferences = listOf(ExternalReference("website", "https://example.com"))
        )
        val time = Instant.now()
        
        val doc1 = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            specVersion = "1.0",
            components = listOf(component),
            creationTime = time
        )
        
        val doc2 = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            specVersion = "1.0",
            components = listOf(component),
            creationTime = time
        )
        
        doc1.id shouldBe doc2.id
    }
})