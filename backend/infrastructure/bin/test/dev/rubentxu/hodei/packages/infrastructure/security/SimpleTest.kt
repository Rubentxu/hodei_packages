package dev.rubentxu.hodei.packages.infrastructure.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SimpleTest : StringSpec({
    // println("SimpleTest: StringSpec body initializing...") // Opcional
    "this is a simple test that should pass" {
        println("SimpleTest: Running 'should pass' test...")
        1 + 1 shouldBe 2
        println("SimpleTest: 'should pass' test completed.")
    }

    "this is a simple test that should fail intentionally" {
        println("SimpleTest: Running 'should fail' test...")
        1 + 1 shouldBe 3 // This will fail
        println("SimpleTest: 'should fail' test completed (should not reach here).")
    }
})
