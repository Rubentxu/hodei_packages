package dev.rubentxu.hodei.packages.domain.registrymanagement.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import java.time.Instant
import java.util.UUID

/**
 * Representa un registro de artefactos en el sistema.
 * Un registro de artefactos es un contenedor lógico para artefactos de un tipo específico (Maven, NPM).
 */
data class Registry(
    val id: UUID,
    val name: String,
    val type: RegistryType,
    val storageType: StorageType,
    val description: String,
    val createdBy: UserId,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPublic: Boolean
) {
    init {
        validate()
    }

    private fun validate() {
        require(name.isNotBlank()) { "ArtifactRegistry name cannot be blank" }
        require(name.matches(NAME_PATTERN)) { "ArtifactRegistry name can only contain alphanumeric characters, hyphens, and underscores" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) { "ArtifactRegistry description cannot exceed 255 characters" }
    }

    companion object {
        private val NAME_PATTERN = Regex("^[a-zA-Z0-9\\-_]+$")
        private const val MAX_DESCRIPTION_LENGTH = 255
    }
} 