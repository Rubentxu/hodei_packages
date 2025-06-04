plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization) // If using kotlinx.serialization for DB DTOs etc.
}

dependencies {
    implementation(project(":backend:domain"))

    implementation(project(":backend:application"))

    // Database

    implementation(libs.postgresql.driver)
    implementation(libs.hikariCP)
    implementation(libs.kotlinx.coroutines.core) // Added for coroutine support (Dispatchers, withContext)

    // JWT - Auth0
    implementation("com.auth0:java-jwt:4.4.0")

    // Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.kotest.runner.junit5) // Note: Original was kotest-runner-junit5-jvm:5.8.0, catalog uses 5.8.1
    testImplementation(libs.kotest.assertions.core) // Note: Original was kotest-assertions-core-jvm:5.8.0, catalog uses 5.8.1
    testImplementation(libs.mockk) // Note: Original was 1.13.10, catalog uses 1.13.11
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.junit.jupiter.api) // Note: Original was 5.9.2, catalog uses 5.10.2
    testRuntimeOnly(libs.junit.jupiter.engine) // Note: Original was 5.9.2, catalog uses 5.10.2

}
