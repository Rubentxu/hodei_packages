package dev.rubentxu.hodei.packages.domain.registrymanagement.model

/**
 * Supported registry types.
 */
enum class RegistryType {
    /**
     * Maven repository for Java/Kotlin artifacts.
     */
    MAVEN,

    /**
     * NPM repository for JavaScript/Node.js packages.
     */
    NPM,

    /**
     * Docker repository for container images.
     */
    DOCKER,

    /**
     * PyPI repository for Python packages.
     */
    PYPI,

    /**
     * NuGet repository for .NET packages.
     */
    NUGET,

    /**
     * Generic repository for raw files or binaries.
     */
    GENERIC,

    /**
     * Helm repository for Helm charts.
     */
    HELM,

    /**
     * Conan repository for C/C++ packages.
     */
    CONAN;

    companion object {
        /**
         * Returns a `RegistryType` from a given string, ignoring case.
         *
         * @param name The name of the registry type.
         * @return The corresponding `RegistryType` or null if not found.
         */
        fun fromString(name: String): RegistryType? =
            values().find { it.name.equals(name, ignoreCase = true) }
    }
} 