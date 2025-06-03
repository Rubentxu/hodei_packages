package dev.rubentxu.hodei.packages.domain.model.sbom

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SbomFormatTest : StringSpec({
    "SbomFormat should define CycloneDX format" {
        val format = SbomFormat.CYCLONE_DX
        format.shouldBeInstanceOf<SbomFormat>()
        format.toString() shouldBe "CYCLONE_DX"
    }

    "SbomFormat should define SPDX format" {
        val format = SbomFormat.SPDX
        format.shouldBeInstanceOf<SbomFormat>()
        format.toString() shouldBe "SPDX"
    }

    "SbomFormat should provide a proper description" {
        SbomFormat.CYCLONE_DX.description shouldBe "CycloneDX"
        SbomFormat.SPDX.description shouldBe "SPDX"
    }

    "SbomFormat should be able to find format by name" {
        SbomFormat.fromString("cyclone_dx") shouldBe SbomFormat.CYCLONE_DX
        SbomFormat.fromString("CYCLONE_DX") shouldBe SbomFormat.CYCLONE_DX
        SbomFormat.fromString("spdx") shouldBe SbomFormat.SPDX
        SbomFormat.fromString("SPDX") shouldBe SbomFormat.SPDX
    }

    "SbomFormat.fromString should handle unknown format values" {
        SbomFormat.fromString("unknown") shouldBe null
    }
})
