package dev.rubentxu.hodei.packages.domain.model.permission

import java.time.Instant
import java.util.UUID

/**
 * Representa la asignación de un rol a un usuario para un recurso específico.
 * Esta entidad relaciona usuarios con sus roles y permisos en diferentes contextos.
 */
data class UserPermission(
    val id: UUID,
    val userId: UUID,
    val roleId: UUID,
    val repositoryId: UUID?,  // Null para roles globales
    val grantedBy: UUID,
    val grantedAt: Instant,
    val expiresAt: Instant?   // Null para permisos permanentes
) {
    init {
        validate()
    }
    
    private fun validate() {
        // Un permiso debe tener un alcance: o es global (repositoryId es null) o específico a un repositorio
        if (expiresAt != null) {
            require(expiresAt.isAfter(grantedAt)) { "Expiration date must be after grant date" }
        }
    }
    
    /**
     * Verifica si el permiso está activo (no ha expirado).
     * @return true si el permiso está activo, false si ha expirado
     */
    fun isActive(): Boolean {
        return expiresAt == null || expiresAt.isAfter(Instant.now())
    }
    
    /**
     * Determina si este permiso aplica al repositorio especificado.
     * @param repoId ID del repositorio a verificar
     * @return true si el permiso aplica al repositorio, false en caso contrario
     */
    fun appliesTo(repoId: UUID): Boolean {
        return repositoryId == null || repositoryId == repoId
    }
    
    /**
     * Crea una copia de este permiso con una nueva fecha de expiración.
     * @param newExpiresAt Nueva fecha de expiración
     * @return Nuevo objeto UserPermission con la fecha actualizada
     */
    fun withNewExpiration(newExpiresAt: Instant?): UserPermission {
        return this.copy(expiresAt = newExpiresAt)
    }
    
    companion object {
        /**
         * Crea un permiso global (aplica a todos los repositorios).
         */
        fun createGlobalPermission(
            userId: UUID,
            roleId: UUID,
            grantedBy: UUID,
            expiresAt: Instant? = null
        ): UserPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = userId,
            roleId = roleId,
            repositoryId = null,
            grantedBy = grantedBy,
            grantedAt = Instant.now(),
            expiresAt = expiresAt
        )
        
        /**
         * Crea un permiso específico para un repositorio.
         */
        fun createRepositoryPermission(
            userId: UUID,
            roleId: UUID,
            repositoryId: UUID,
            grantedBy: UUID,
            expiresAt: Instant? = null
        ): UserPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = userId,
            roleId = roleId,
            repositoryId = repositoryId,
            grantedBy = grantedBy,
            grantedAt = Instant.now(),
            expiresAt = expiresAt
        )
    }
}