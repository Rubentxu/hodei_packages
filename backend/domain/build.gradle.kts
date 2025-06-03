

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    // This module should remain free of framework dependencies.

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.assertions.core) // Added for Kotest assertions like shouldBe
    testImplementation(libs.kotest.runner.junit5 ) // Added for Kotest assertions like shouldBe
}
