plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    `java-library`
}

allprojects {
    repositories {
        mavenCentral()
    }
}
