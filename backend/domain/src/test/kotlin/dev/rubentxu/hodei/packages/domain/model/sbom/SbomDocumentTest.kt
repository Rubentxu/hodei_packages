package dev.rubentxu.hodei.packages.domain.model.sbom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.Instant

class SbomDocumentTest : StringSpec({
    "SbomDocument should be created with required fields" {
        val component1 = SbomComponent("package1", "1.0.0", "library")
        val component2 = SbomComponent("package2", "2.0.0", "library")
        
        val doc = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            components = listOf(component1, component2)
        )
        
        doc.artifactId shouldBe "test-artifact-123"
        doc.format shouldBe SbomFormat.CYCLONE_DX
        doc.components shouldBe listOf(component1, component2)
        doc.id.shouldNotBeBlank()
        doc.version shouldBe "1.0"
        doc.creationTime shouldNotBe null
        doc.relationships shouldBe emptyList()
    }
    
    "SbomDocument should support optional fields" {
        val component1 = SbomComponent("package1", "1.0.0", "library")
        val component2 = SbomComponent("package2", "2.0.0", "library")
        
        val relationship = SbomRelationship(component1.id, component2.id, "DEPENDS_ON")
        val creationTime = Instant.now().minusSeconds(3600)
        
        val doc = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            components = listOf(component1, component2),
            relationships = listOf(relationship),
            version = "2.1",
            creationTime = creationTime
        )
        
        doc.version shouldBe "2.1"
        doc.creationTime shouldBe creationTime
        doc.relationships shouldContain relationship
    }
    
    "SbomDocument should validate artifactId is not blank" {
        val component = SbomComponent("package1", "1.0.0", "library")
        
        val exception = shouldThrow<IllegalArgumentException> {
            SbomDocument(
                artifactId = "",
                format = SbomFormat.CYCLONE_DX,
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
                components = emptyList()
            )
        }
        
        exception.message shouldBe "Components list cannot be empty"
    }
    
    "SbomDocument should generate consistent id based on artifactId and creation time" {
        val component = SbomComponent("package1", "1.0.0", "library")
        val time = Instant.now()
        
        val doc1 = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            components = listOf(component),
            creationTime = time
        )
        
        val doc2 = SbomDocument(
            artifactId = "test-artifact-123",
            format = SbomFormat.CYCLONE_DX,
            components = listOf(component),
            creationTime = time
        )
        
        doc1.id shouldBe doc2.id
    }
})
