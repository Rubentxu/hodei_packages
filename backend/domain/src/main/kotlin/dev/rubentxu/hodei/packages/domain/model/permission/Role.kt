package dev.rubentxu.hodei.packages.domain.model.permission

import java.time.Instant
import java.util.UUID

/**
 * Representa un rol en el sistema que agrupa un conjunto de permisos.
 * Los roles pueden ser predefinidos (ADMIN, CONTRIBUTOR, READER) o personalizados.
 */
data class Role(
    val id: UUID,
    val name: String,
    val description: String,
    val permissions: Set<Permission>,
    val isSystemRole: Boolean,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        validate()
    }
    
    private fun validate() {
        require(name.isNotBlank()) { "Role name cannot be blank" }
        require(name.matches(NAME_PATTERN)) { "Role name can only contain alphanumeric characters, hyphens, and underscores" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) { "Role description cannot exceed 255 characters" }
        require(permissions.isNotEmpty()) { "Role must have at least one permission" }
    }
    
    companion object {
        private val NAME_PATTERN = Regex("^[a-zA-Z0-9\\-_]+$")
        private const val MAX_DESCRIPTION_LENGTH = 255
        
        /**
         * Crea un rol de administrador predefinido.
         */
        fun createAdminRole(createdBy: UUID): Role = Role(
            id = UUID.randomUUID(),
            name = "ADMIN",
            description = "Administrator role with full access to all features",
            permissions = Permission.ADMIN_PERMISSIONS,
            isSystemRole = true,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        /**
         * Crea un rol de contribuidor predefinido.
         */
        fun createContributorRole(createdBy: UUID): Role = Role(
            id = UUID.randomUUID(),
            name = "CONTRIBUTOR",
            description = "Contributor role with permission to upload and manage artifacts",
            permissions = Permission.CONTRIBUTOR_PERMISSIONS,
            isSystemRole = true,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        /**
         * Crea un rol de lector predefinido.
         */
        fun createReaderRole(createdBy: UUID): Role = Role(
            id = UUID.randomUUID(),
            name = "READER",
            description = "Reader role with permission to view and download artifacts",
            permissions = Permission.READER_PERMISSIONS,
            isSystemRole = true,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}