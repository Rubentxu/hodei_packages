package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SbomFormatTest : StringSpec({
    "SbomFormat should define CycloneDX format" {
        val format = SbomFormat.CYCLONE_DX
        format.shouldBeInstanceOf<SbomFormat>()
        format.toString() shouldBe "CYCLONE_DX"
        format.officialName shouldBe "CycloneDX"
        format.description shouldBe "CycloneDX SBOM Format"
    }

    "SbomFormat should define SPDX format" {
        val format = SbomFormat.SPDX
        format.shouldBeInstanceOf<SbomFormat>()
        format.toString() shouldBe "SPDX"
        format.officialName shouldBe "SPDX"
        format.description shouldBe "SPDX SBOM Format"
    }

    "SbomFormat should provide a proper description" {
        SbomFormat.CYCLONE_DX.description shouldBe "CycloneDX SBOM Format"
        SbomFormat.SPDX.description shouldBe "SPDX SBOM Format"
    }

    "SbomFormat should be able to find format by name" {
        SbomFormat.fromString("cyclone_dx") shouldBe SbomFormat.CYCLONE_DX
        SbomFormat.fromString("CYCLONE_DX") shouldBe SbomFormat.CYCLONE_DX
        SbomFormat.fromString("spdx") shouldBe SbomFormat.SPDX
        SbomFormat.fromString("SPDX") shouldBe SbomFormat.SPDX
    }

    "SbomFormat.fromString should find format by enum name, official name, or description substring" {
        val result1 = SbomFormat.safeFromString("cyclone_dx")
        result1.isSuccess shouldBe true
        result1.getOrNull() shouldBe SbomFormat.CYCLONE_DX

        val result2 = SbomFormat.safeFromString("CYCLONE_DX")
        result2.isSuccess shouldBe true
        result2.getOrNull() shouldBe SbomFormat.CYCLONE_DX

        val result3 = SbomFormat.safeFromString("CycloneDX")
        result3.isSuccess shouldBe true
        result3.getOrNull() shouldBe SbomFormat.CYCLONE_DX

        val result4 = SbomFormat.safeFromString("CycloneDX SBOM Format")
        result4.isSuccess shouldBe true
        result4.getOrNull() shouldBe SbomFormat.CYCLONE_DX

        val result5 = SbomFormat.safeFromString("spdx")
        result5.isSuccess shouldBe true
        result5.getOrNull() shouldBe SbomFormat.SPDX

        val result6 = SbomFormat.safeFromString("SPDX")
        result6.isSuccess shouldBe true
        result6.getOrNull() shouldBe SbomFormat.SPDX

        val result7 = SbomFormat.safeFromString("SPDX SBOM Format")
        result7.isSuccess shouldBe true
        result7.getOrNull() shouldBe SbomFormat.SPDX
    }

    "SbomFormat.safeFromString should handle unknown format values and return Failure" {
        val result = SbomFormat.safeFromString("unknown")
        result.isFailure shouldBe true
        val exception = result.exceptionOrNull()
        exception?.shouldBeInstanceOf<IllegalArgumentException>()
        exception?.message shouldBe "Unknown SbomFormat: unknown"
    }
})