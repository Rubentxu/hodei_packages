package dev.rubentxu.hodei.packages.domain.model.repository

import java.time.Instant
import java.util.UUID

/**
 * Representa un repositorio de artefactos en el sistema.
 * Un repositorio es un contenedor lógico para artefactos de un tipo específico (Maven, NPM).
 */
data class Repository(
    val id: UUID,
    val name: String,
    val type: RepositoryType,
    val storageType: StorageType, // Added storageType field
    val description: String,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPublic: Boolean
) {
    init {
        validate()
    }
    
    private fun validate() {
        require(name.isNotBlank()) { "Repository name cannot be blank" }
        require(name.matches(NAME_PATTERN)) { "Repository name can only contain alphanumeric characters, hyphens, and underscores" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) { "Repository description cannot exceed 255 characters" }
    }
    
    companion object {
        private val NAME_PATTERN = Regex("^[a-zA-Z0-9\\-_]+$")
        private const val MAX_DESCRIPTION_LENGTH = 255
    }
}