plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    `java-library`
}

allprojects {
    repositories {
        mavenCentral()
    }
//    tasks.register("prepareKotlinBuildScriptModel") { }

    tasks.withType<Test> {
        useJUnitPlatform() // Para que Kotest funcione correctamente
        testLogging {
            events("passed", "skipped", "failed") // Muestra eventos de test en la consola
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL // Muestra stack traces completos
            showStandardStreams = true // Muestra stdout/stderr de los tests
        }
    }

}
