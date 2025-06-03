import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    `java-library`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    
    // Configuración de toolchain
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    dependencies {
        // Kotlin
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")



        // Testing
        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
        testImplementation("io.kotest:kotest-assertions-core:5.8.1")
        testImplementation("io.mockk:mockk:1.13.11")
        
        constraints {
            implementation("org.jetbrains.kotlin:kotlin-test-junit5") {
                because("Prefer JUnit5 support for Kotlin tests")
            }
        }
    }
}
