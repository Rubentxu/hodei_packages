package dev.rubentxu.hodei.packages.domain.policymanagement.service

import dev.rubentxu.hodei.packages.domain.model.permission.Role
import dev.rubentxu.hodei.packages.domain.model.permission.UserPermission
import dev.rubentxu.hodei.packages.domain.policymanagement.events.PermissionEvent
import dev.rubentxu.hodei.packages.domain.policymanagement.model.Permission
import dev.rubentxu.hodei.packages.domain.policymanagement.ports.PermissionRepository
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import java.time.Instant
import java.util.*

class PermissionService(
    private val permissionRepository: PermissionRepository,
    private val registryRepository: RegistryRepository,
    private val eventPublisher: (PermissionEvent) -> Unit
) {
    suspend fun createRole(
        name: String,
        description: String,
        permissions: Set<Permission>,
        isSystemRole: Boolean = false,
        createdBy: UUID
    ): Result<Role> {
        return permissionRepository.findRoleByName(name).flatMap { existingRole ->
            if (existingRole != null) {
                Result.failure(IllegalStateException("A role with name '$name' already exists"))
            } else {
                val now = Instant.now()
                val role = Role(
                    id = UUID.randomUUID(),
                    name = name,
                    description = description,
                    permissions = permissions,
                    isSystemRole = isSystemRole,
                    createdBy = createdBy,
                    createdAt = now,
                    updatedAt = now
                )
                
                permissionRepository.saveRole(role).onSuccess { savedRole ->
                    eventPublisher(
                        PermissionEvent.RoleCreated(
                            roleId = savedRole.id,
                            name = savedRole.name,
                            isSystemRole = savedRole.isSystemRole,
                            createdBy = savedRole.createdBy,
                            timestamp = savedRole.createdAt
                        )
                    )
                }
            }
        }
    }

    suspend fun updateRole(
        id: UUID,
        description: String? = null,
        permissions: Set<Permission>? = null,
        updatedBy: UUID
    ): Result<Role> {
        return permissionRepository.findRoleById(id).flatMap { role ->
            when {
                role == null -> Result.failure(IllegalArgumentException("Role with ID '$id' not found"))
                role.isSystemRole -> Result.failure(IllegalStateException("System roles cannot be modified"))
                else -> {
                    val changes = mutableMapOf<String, Any?>()
                    val updatedRole = role.copy(
                        description = description?.also { changes["description"] = it } ?: role.description,
                        permissions = permissions?.also { changes["permissions"] = it } ?: role.permissions,
                        updatedAt = Instant.now()
                    )
                    
                    if (changes.isEmpty()) {
                        Result.success(role)
                    } else {
                        permissionRepository.saveRole(updatedRole).onSuccess { savedRole ->
                            eventPublisher(
                                PermissionEvent.RoleUpdated(
                                    roleId = savedRole.id,
                                    name = savedRole.name,
                                    updatedBy = updatedBy,
                                    timestamp = savedRole.updatedAt,
                                    changes = changes
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteRole(id: UUID, deletedBy: UUID): Result<Boolean> {
        return permissionRepository.findRoleById(id).flatMap { role ->
            when {
                role == null -> Result.success(false)
                role.isSystemRole -> Result.failure(IllegalStateException("System roles cannot be deleted"))
                else -> {
                    val isRoleInUse = isRoleInUse(id)
                    if (isRoleInUse) {
                        Result.failure(IllegalStateException("Cannot delete role '${role.name}' because it is assigned to users"))
                    } else {
                        permissionRepository.deleteRoleById(id).onSuccess { deleted ->
                            if (deleted) {
                                eventPublisher(
                                    PermissionEvent.RoleDeleted(
                                        roleId = role.id,
                                        name = role.name,
                                        deletedBy = deletedBy,
                                        timestamp = Instant.now()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun isRoleInUse(roleId: UUID): Boolean {
        val allUserIds = findAllUserIds()
        for (userId in allUserIds) {
            permissionRepository.findPermissionsByUserId(userId).onSuccess { permissions ->
                if (permissions.any { it.roleId == roleId }) {
                    return true
                }
            }
        }
        return false
    }

    private suspend fun findAllUserIds(): List<UUID> {
        // En un sistema real, esta información vendría de un repositorio de usuarios
        return emptyList() // Implementación temporal
    }

    suspend fun grantGlobalPermission(
        userId: UUID,
        roleId: UUID,
        grantedBy: UUID,
        expiresAt: Instant? = null
    ): Result<UserPermission> {
        return permissionRepository.findRoleById(roleId).flatMap { role ->
            if (role == null) {
                Result.failure(IllegalArgumentException("Role with ID '$roleId' not found"))
            } else {
                val userPermission = UserPermission.Companion.createGlobalPermission(
                    userId = userId,
                    roleId = roleId,
                    grantedBy = grantedBy,
                    expiresAt = expiresAt
                )
                
                permissionRepository.saveUserPermission(userPermission).onSuccess { savedPermission ->
                    eventPublisher(
                        PermissionEvent.PermissionGranted(
                            permissionId = savedPermission.id,
                            userId = savedPermission.userId,
                            roleId = savedPermission.roleId,
                            roleName = role.name,
                            repositoryId = savedPermission.registryId,
                            repositoryName = null,
                            grantedBy = savedPermission.grantedBy,
                            expiresAt = savedPermission.expiresAt,
                            timestamp = savedPermission.grantedAt
                        )
                    )
                }
            }
        }
    }

    suspend fun grantRepositoryPermission(
        userId: UUID,
        roleId: UUID,
        repositoryId: UUID,
        grantedBy: UUID,
        expiresAt: Instant? = null
    ): Result<UserPermission> {
        val roleResult = permissionRepository.findRoleById(roleId)
        val role = roleResult.getOrNull() 
            ?: return Result.failure(IllegalArgumentException("Role with ID '$roleId' not found"))
        
        return registryRepository.findById(repositoryId).flatMap { repository ->
            if (repository == null) {
                Result.failure(IllegalArgumentException("ArtifactRegistry with ID '$repositoryId' not found"))
            } else {
                val userPermission = UserPermission.Companion.createRepositoryPermission(
                    userId = userId,
                    roleId = roleId,
                    repositoryId = repositoryId,
                    grantedBy = grantedBy,
                    expiresAt = expiresAt
                )
                
                permissionRepository.saveUserPermission(userPermission).onSuccess { savedPermission ->
                    eventPublisher(
                        PermissionEvent.PermissionGranted(
                            permissionId = savedPermission.id,
                            userId = savedPermission.userId,
                            roleId = savedPermission.roleId,
                            roleName = role.name,
                            repositoryId = savedPermission.registryId,
                            repositoryName = repository.name,
                            grantedBy = savedPermission.grantedBy,
                            expiresAt = savedPermission.expiresAt,
                            timestamp = savedPermission.grantedAt
                        )
                    )
                }
            }
        }
    }

    suspend fun revokePermission(permissionId: UUID, revokedBy: UUID): Result<Boolean> {
        val permissionResult = findPermissionById(permissionId)
        val permission = permissionResult.getOrNull() ?: return Result.success(false)
        
        return permissionRepository.findRoleById(permission.roleId).flatMap { role ->
            if (role == null) {
                Result.failure(IllegalStateException("Role with ID '${permission.roleId}' not found"))
            } else {
                permissionRepository.revokePermission(permissionId).map { revoked ->
                    if (revoked) {
                        val repositoryName = permission.registryId?.let { repoId ->
                            registryRepository.findById(repoId).getOrNull()?.name
                        }
                        
                        eventPublisher(
                            PermissionEvent.PermissionRevoked(
                                permissionId = permission.id,
                                userId = permission.userId,
                                roleId = permission.roleId,
                                roleName = role.name,
                                repositoryId = permission.registryId,
                                repositoryName = repositoryName,
                                revokedBy = revokedBy,
                                timestamp = Instant.now()
                            )
                        )
                    }
                    revoked
                }
            }
        }
    }

    private suspend fun findPermissionById(permissionId: UUID): Result<UserPermission> {
        val allUserIds = findAllUserIds()
        for (userId in allUserIds) {
            val permissionsResult = permissionRepository.findPermissionsByUserId(userId)
            val permissions = permissionsResult.getOrNull() ?: continue
            val permission = permissions.find { it.id == permissionId }
            if (permission != null) {
                return Result.success(permission)
            }
        }
        return Result.failure(NoSuchElementException("Permission with ID '$permissionId' not found"))
    }

    suspend fun updatePermissionExpiration(
        permissionId: UUID,
        newExpiresAt: Instant?,
        updatedBy: UUID
    ): Result<UserPermission> {
        val permissionResult = findPermissionById(permissionId)
        if (permissionResult.isFailure) {
            return Result.failure(IllegalArgumentException("Permission with ID '$permissionId' not found"))
        }
        
        val permission = permissionResult.getOrThrow()
        if (permission.expiresAt != newExpiresAt) {
            val updatedPermission = permission.withNewExpiration(newExpiresAt)
            return permissionRepository.saveUserPermission(updatedPermission).onSuccess { savedPermission ->
                eventPublisher(
                    PermissionEvent.PermissionExpirationChanged(
                        permissionId = savedPermission.id,
                        userId = savedPermission.userId,
                        roleId = savedPermission.roleId,
                        repositoryId = savedPermission.registryId,
                        newExpiresAt = savedPermission.expiresAt,
                        updatedBy = updatedBy,
                        timestamp = Instant.now()
                    )
                )
            }
        }
        
        return Result.success(permission)
    }

    suspend fun hasPermission(
        userId: UUID,
        permission: Permission,
        repositoryId: UUID? = null
    ): Result<Boolean> {
        return permissionRepository.findPermissionsByUserId(userId, activeOnly = true).map { permissions ->
            // Verificar permisos globales primero
            val hasGlobalPermission = permissions
                .filter { it.registryId == null }
                .any { userPermission ->
                    val roleResult = permissionRepository.findRoleById(userPermission.roleId).getOrNull()
                    roleResult?.permissions?.contains(permission) == true
                }
            
            if (hasGlobalPermission) {
                true
            } else if (repositoryId != null) {
                // Si no tiene permisos globales, verificar permisos específicos para el repositorio
                permissions
                    .filter { it.registryId == null || it.registryId == repositoryId }
                    .any { userPermission ->
                        val roleResult = permissionRepository.findRoleById(userPermission.roleId).getOrNull()
                        roleResult?.permissions?.contains(permission) == true
                    }
            } else {
                false
            }
        }
    }

    suspend fun getAllRoles(): Result<List<Role>> {
        return permissionRepository.findAllRoles()
    }

    suspend fun findUserPermissions(
        userId: UUID? = null,
        repositoryId: UUID? = null,
        roleId: UUID? = null,
        activeOnly: Boolean = true
    ): Result<List<UserPermission>> {
        if (userId != null) {
            return permissionRepository.findPermissionsByUserId(userId, activeOnly).map { permissions ->
                permissions.filter { permission ->
                    (repositoryId == null || permission.registryId == repositoryId) &&
                            (roleId == null || permission.roleId == roleId)
                }
            }
        } 
        
        if (repositoryId != null) {
            return permissionRepository.findPermissionsByRepositoryId(repositoryId, activeOnly).map { permissions ->
                permissions.filter { permission ->
                    roleId == null || permission.roleId == roleId
                }
            }
        } 
        
        val result = mutableListOf<UserPermission>()
        val allUserIds = findAllUserIds()
        
        for (uId in allUserIds) {
            permissionRepository.findPermissionsByUserId(uId, activeOnly).onSuccess { permissions ->
                result.addAll(permissions.filter { permission ->
                    roleId == null || permission.roleId == roleId
                })
            }
        }
        
        return Result.success(result)
    }
}

// Extensiones para usar Result de forma más funcional
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return fold(
        onSuccess = { Result.success(transform(it)) },
        onFailure = { Result.failure(it) }
    )
}

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}
