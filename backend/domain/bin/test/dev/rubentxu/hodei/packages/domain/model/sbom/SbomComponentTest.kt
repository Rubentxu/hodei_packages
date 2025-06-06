package dev.rubentxu.hodei.packages.domain.model.sbom

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomComponent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank

class SbomComponentTest : StringSpec({
    "SbomComponent should be created with required fields" {
        val component = SbomComponent(
            name = "test-package",
            version = "1.0.0",
            type = "library"
        )

        component.name shouldBe "test-package"
        component.version shouldBe "1.0.0"
        component.type shouldBe "library"
        component.id.shouldNotBeBlank()
        component.licenses shouldBe emptyList()
        component.description shouldBe null
    }

    "SbomComponent should support optional fields" {
        val component = SbomComponent(
            name = "test-package",
            version = "1.0.0",
            type = "library",
            licenses = listOf("MIT", "Apache-2.0"),
            description = "Test component"
        )

        component.name shouldBe "test-package"
        component.licenses shouldBe listOf("MIT", "Apache-2.0")
        component.description shouldBe "Test component"
    }

    "SbomComponent should generate consistent id based on name and version" {
        val component1 = SbomComponent(
            name = "test-package",
            version = "1.0.0",
            type = "library"
        )

        val component2 = SbomComponent(
            name = "test-package",
            version = "1.0.0",
            type = "library"
        )

        val component3 = SbomComponent(
            name = "test-package",
            version = "2.0.0",
            type = "library"
        )

        component1.id shouldBe component2.id
        component1.id shouldNotBe component3.id
    }

    "SbomComponent should validate name is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            SbomComponent(
                name = "",
                version = "1.0.0",
                type = "library"
            )
        }
        exception.message shouldBe "Component name cannot be blank"
    }

    "SbomComponent should validate version is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            SbomComponent(
                name = "test-package",
                version = "",
                type = "library"
            )
        }
        exception.message shouldBe "Component version cannot be blank"
    }

    "SbomComponent should validate type is not blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            SbomComponent(
                name = "test-package",
                version = "1.0.0", 
                type = ""
            )
        }
        exception.message shouldBe "Component type cannot be blank"
    }
})
