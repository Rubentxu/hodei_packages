package dev.rubentxu.hodei.packages.domain.artifactmanagement.model

/**
 * Defines the supported formats for a repository.
 * Each format typically corresponds to a specific package manager or artifact type.
 */
enum class ArtifactType {
    /**
     * Maven repository format, commonly used for Java/Kotlin JVM artifacts.
     */
    MAVEN,

    /**
     * NPM repository format, for JavaScript/Node.js packages.
     */

    NPM,

    /**
     * Docker repository format, for container images.
     */
    DOCKER,

    /**
     * PyPI (Python Package Index) repository format, for Python packages.
     */
    PYPI,

    /**
     * NuGet repository format, for .NET packages.
     */
    NUGET,

    /**
     * Generic repository format, suitable for storing and distributing raw files or binaries
     * without specific package metadata interpretation.
     */
    GENERIC,

    /**
     * Helm repository format, for Kubernetes Helm charts.
     */
    HELM,

    /**
     * Conan repository format, for C/C++ packages.
     */
    CONAN,

    /**
     * RubyGems repository format, for Ruby gems.
     */
    RUBYGEMS, // Added

    /**
     * Composer repository format, for PHP packages.
     */
    COMPOSER, // Added

    /**
     * Crates.io repository format, for Rust crates.
     */
    CRATES,   // Added

    /**
     * Go modules format.
     */
    GO;       // Added

    companion object {
        /**
         * Returns a `RegistryFormat` from a given string, ignoring case.
         *
         * @param name The name of the registry format.
         * @return The corresponding `RegistryFormat` or `null` if no match is found.
         */
        fun fromString(name: String): ArtifactType? =
            values().find { it.name.equals(name, ignoreCase = true) }

        /**
         * Returns a `RegistryFormat` from a given string, ignoring case, or throws an exception.
         *
         * @param name The name of the registry format.
         * @return The corresponding `RegistryFormat`.
         * @throws IllegalArgumentException if no match is found for the given name.
         */
        fun fromStringOrThrow(name: String): ArtifactType =
            fromString(name)
                ?: throw IllegalArgumentException("Unknown RegistryFormat: '$name'. Supported formats are: ${values().joinToString { it.name }}")
    }
}