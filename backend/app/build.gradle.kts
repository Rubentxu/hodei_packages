plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("dev.rubentxu.hodei.packages.app.Application")
}

dependencies {
    implementation(project(":backend:application"))
    implementation(project(":backend:domain"))
    implementation(project(":backend:infrastructure"))
    
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:3.1.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.3")
    implementation("io.ktor:ktor-server-request-validation:3.1.3")
    implementation("io.ktor:ktor-server-auth:3.1.3")
    implementation("io.ktor:ktor-server-status-pages:3.1.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.1.3")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:3.1.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation(kotlin("test-junit5"))

    // Koin for dependency injection
    implementation("io.insert-koin:koin-ktor:3.5.0")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.0")
    testImplementation("io.insert-koin:koin-test:3.5.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }
    testImplementation("io.insert-koin:koin-test-junit5:3.5.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }


    // MockK for mocking
    testImplementation("io.mockk:mockk:1.13.11")

    // Kotlinx Coroutines Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform() // Para que Kotest funcione correctamente
    testLogging {
        events("passed", "skipped", "failed") // Muestra eventos de test en la consola
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL // Muestra stack traces completos
        showStandardStreams = true // Muestra stdout/stderr de los tests
    }
}
