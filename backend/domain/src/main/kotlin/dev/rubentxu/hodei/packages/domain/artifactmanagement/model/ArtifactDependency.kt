package dev.rubentxu.hodei.packages.domain.artifactmanagement.model

/**
 * Represents an artifact dependency, which can be used to build
 * dependency graphs and verify compatibilities.
 *
 * @property coordinates The coordinates of the artifact it depends on.
 * @property scope The scope of the dependency (compile, runtime, test, etc.).
 * @property isOptional Whether the dependency is optional.
 * @property versionConstraint The version constraint (can be an exact version or a range).
 */
data class ArtifactDependency(
    val coordinates: ArtifactCoordinates,
    val scope: String? = null,
    val isOptional: Boolean = false,
    val versionConstraint: String? = null
)