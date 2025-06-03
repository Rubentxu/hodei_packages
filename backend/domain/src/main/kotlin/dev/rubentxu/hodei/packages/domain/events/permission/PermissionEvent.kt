package dev.rubentxu.hodei.packages.domain.events.permission

import java.time.Instant
import java.util.UUID

/**
 * Eventos de dominio relacionados con operaciones de permisos y roles.
 * Estos eventos son emitidos cuando ocurren cambios significativos en los permisos o roles.
 */
sealed class PermissionEvent {
    /**
     * Evento emitido cuando se crea un nuevo rol.
     */
    data class RoleCreated(
        val roleId: UUID,
        val name: String,
        val isSystemRole: Boolean,
        val createdBy: UUID,
        val timestamp: Instant,
    ) : PermissionEvent()

    /**
     * Evento emitido cuando se actualiza un rol existente.
     */
    data class RoleUpdated(
        val roleId: UUID,
        val name: String,
        val updatedBy: UUID,
        val timestamp: Instant,
        val changes: Map<String, Any?>,
    ) : PermissionEvent()

    /**
     * Evento emitido cuando se elimina un rol.
     */
    data class RoleDeleted(
        val roleId: UUID,
        val name: String,
        val deletedBy: UUID,
        val timestamp: Instant,
    ) : PermissionEvent()

    /**
     * Evento emitido cuando se concede un permiso a un usuario.
     */
    data class PermissionGranted(
        val permissionId: UUID,
        val userId: UUID,
        val roleId: UUID,
        val roleName: String,
        val repositoryId: UUID?,
        val repositoryName: String?,
        val grantedBy: UUID,
        val timestamp: Instant,
        val expiresAt: Instant?,
    ) : PermissionEvent()

    /**
     * Evento emitido cuando se revoca un permiso a un usuario.
     */
    data class PermissionRevoked(
        val permissionId: UUID,
        val userId: UUID,
        val roleId: UUID,
        val roleName: String,
        val repositoryId: UUID?,
        val repositoryName: String?,
        val revokedBy: UUID,
        val timestamp: Instant,
    ) : PermissionEvent()

    /**
     * Evento emitido cuando se modifica la fecha de expiración de un permiso.
     */
    data class PermissionExpirationChanged(
        val permissionId: UUID,
        val userId: UUID,
        val roleId: UUID,
        val repositoryId: UUID?,
        val updatedBy: UUID,
        val timestamp: Instant,
        val newExpiresAt: Instant?,
    ) : PermissionEvent()
}