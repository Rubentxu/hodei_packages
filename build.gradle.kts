plugins {
    kotlin("jvm") version "2.1.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    `java-library`
}

allprojects {
    repositories {
        mavenCentral()
    }
}
