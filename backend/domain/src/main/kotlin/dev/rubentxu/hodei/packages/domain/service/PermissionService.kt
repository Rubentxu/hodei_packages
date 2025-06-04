package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.permission.PermissionEvent
import dev.rubentxu.hodei.packages.domain.model.permission.Permission
import dev.rubentxu.hodei.packages.domain.model.permission.Role
import dev.rubentxu.hodei.packages.domain.model.permission.UserPermission
import dev.rubentxu.hodei.packages.domain.repository.PermissionRepository
import dev.rubentxu.hodei.packages.domain.repository.ArtifactRegistryRepository
import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con permisos y roles.
 * Gestiona la creación, actualización y validación de roles y permisos de usuario.
 */
class PermissionService(
    private val permissionRepository: PermissionRepository,
    private val repositoryRepository: ArtifactRegistryRepository,
    private val eventPublisher: (PermissionEvent) -> Unit
) {
    /**
     * Crea un nuevo rol en el sistema.
     * @param name Nombre del rol
     * @param description Descripción del rol
     * @param permissions Conjunto de permisos asignados al rol
     * @param isSystemRole Indica si es un rol de sistema (protegido)
     * @param createdBy ID del usuario que crea el rol
     * @return El rol creado
     * @throws IllegalStateException si ya existe un rol con el mismo nombre
     */
    suspend fun createRole(
        name: String,
        description: String,
        permissions: Set<Permission>,
        isSystemRole: Boolean = false,
        createdBy: UUID
    ): Role {
        // Verificar si ya existe un rol con el mismo nombre
        if (permissionRepository.findRoleByName(name) != null) {
            throw IllegalStateException("A role with name '$name' already exists")
        }
        
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
        
        // Persistir el rol
        val savedRole = permissionRepository.saveRole(role)
        
        // Publicar evento de creación de rol
        eventPublisher(
            PermissionEvent.RoleCreated(
                roleId = savedRole.id,
                name = savedRole.name,
                isSystemRole = savedRole.isSystemRole,
                createdBy = savedRole.createdBy,
                timestamp = savedRole.createdAt
            )
        )
        
        return savedRole
    }
    
    /**
     * Actualiza un rol existente.
     * @param id ID del rol a actualizar
     * @param description Nueva descripción (opcional)
     * @param permissions Nuevo conjunto de permisos (opcional)
     * @param updatedBy ID del usuario que realiza la actualización
     * @return El rol actualizado
     * @throws IllegalArgumentException si el rol no existe
     * @throws IllegalStateException si se intenta modificar un rol de sistema
     */
    suspend fun updateRole(
        id: UUID,
        description: String? = null,
        permissions: Set<Permission>? = null,
        updatedBy: UUID
    ): Role {
        val role = permissionRepository.findRoleById(id)
            ?: throw IllegalArgumentException("Role with ID '$id' not found")
        
        // No permitir la modificación de roles de sistema
        if (role.isSystemRole) {
            throw IllegalStateException("System roles cannot be modified")
        }
        
        val changes = mutableMapOf<String, Any?>()
        
        // Crear una nueva instancia con los cambios aplicados
        val updatedRole = role.copy(
            description = description?.also { changes["description"] = it } ?: role.description,
            permissions = permissions?.also { changes["permissions"] = it } ?: role.permissions,
            updatedAt = Instant.now()
        )
        
        // Solo persistir si hay cambios
        if (changes.isNotEmpty()) {
            val savedRole = permissionRepository.saveRole(updatedRole)
            
            // Publicar evento de actualización
            eventPublisher(
                PermissionEvent.RoleUpdated(
                    roleId = savedRole.id,
                    name = savedRole.name,
                    updatedBy = updatedBy,
                    timestamp = savedRole.updatedAt,
                    changes = changes
                )
            )
            
            return savedRole
        }
        
        return role
    }
    
    /**
     * Elimina un rol del sistema.
     * @param id ID del rol a eliminar
     * @param deletedBy ID del usuario que realiza la eliminación
     * @return true si se eliminó correctamente, false si no existía
     * @throws IllegalStateException si se intenta eliminar un rol de sistema o un rol en uso
     */
    suspend fun deleteRole(id: UUID, deletedBy: UUID): Boolean {
        val role = permissionRepository.findRoleById(id)
            ?: return false
        
        // No permitir la eliminación de roles de sistema
        if (role.isSystemRole) {
            throw IllegalStateException("System roles cannot be deleted")
        }
        
        // Verificar si el rol está siendo utilizado por algún permiso de usuario
        val isRoleInUse = isRoleInUse(id)
        
        if (isRoleInUse) {
            throw IllegalStateException("Cannot delete role '${role.name}' because it is assigned to users")
        }
        
        val deleted = permissionRepository.deleteRoleById(id)
        
        if (deleted) {
            // Publicar evento de eliminación
            eventPublisher(
                PermissionEvent.RoleDeleted(
                    roleId = role.id,
                    name = role.name,
                    deletedBy = deletedBy,
                    timestamp = Instant.now()
                )
            )
        }
        
        return deleted
    }
    
    /**
     * Verifica si un rol está siendo utilizado por algún permiso de usuario.
     * @param roleId ID del rol a verificar
     * @return true si el rol está en uso, false en caso contrario
     */
    private suspend fun isRoleInUse(roleId: UUID): Boolean {
        // Comprobamos en todos los usuarios con permisos activos
        val allUserIds = findAllUserIds()
        for (userId in allUserIds) {
            val userPermissions = permissionRepository.findPermissionsByUserId(userId)
            if (userPermissions.any { it.roleId == roleId }) {
                return true
            }
        }
        return false
    }
    
    /**
     * Obtiene todos los IDs de usuarios que tienen permisos en el sistema.
     * Implementación temporal - en un sistema real, esto debería ser más eficiente.
     */
    private suspend fun findAllUserIds(): List<UUID> {
        // En un sistema real, esta información vendría de un repositorio de usuarios
        // o de una consulta específica en el repositorio de permisos
        return emptyList() // Implementación temporal
    }
    
    /**
     * Otorga un permiso global a un usuario.
     * @param userId ID del usuario
     * @param roleId ID del rol a asignar
     * @param grantedBy ID del usuario que otorga el permiso
     * @param expiresAt Fecha de expiración (opcional)
     * @return El permiso creado
     */
    suspend fun grantGlobalPermission(
        userId: UUID,
        roleId: UUID,
        grantedBy: UUID,
        expiresAt: Instant? = null
    ): UserPermission {
        // Verificar que el rol existe
        val role = permissionRepository.findRoleById(roleId)
            ?: throw IllegalArgumentException("Role with ID '$roleId' not found")
        
        val userPermission = if (expiresAt != null) {
            UserPermission(
                id = UUID.randomUUID(),
                userId = userId,
                roleId = roleId,
                repositoryId = null, // Global permission
                grantedBy = grantedBy,
                grantedAt = Instant.now(),
                expiresAt = expiresAt
            )
        } else {
            UserPermission.createGlobalPermission(
                userId = userId,
                roleId = roleId,
                grantedBy = grantedBy
            )
        }
        
        // Persistir el permiso
        val savedPermission = permissionRepository.saveUserPermission(userPermission)
        
        // Publicar evento de otorgamiento de permiso
        eventPublisher(
            PermissionEvent.PermissionGranted(
                permissionId = savedPermission.id,
                userId = savedPermission.userId,
                roleId = savedPermission.roleId,
                roleName = role.name,
                repositoryId = null,
                repositoryName = null,
                grantedBy = savedPermission.grantedBy,
                expiresAt = savedPermission.expiresAt,
                timestamp = savedPermission.grantedAt
            )
        )
        
        return savedPermission
    }
    
    /**
     * Otorga un permiso específico a un usuario para un repositorio.
     * @param userId ID del usuario
     * @param roleId ID del rol a asignar
     * @param repositoryId ID del repositorio
     * @param grantedBy ID del usuario que otorga el permiso
     * @param expiresAt Fecha de expiración (opcional)
     * @return El permiso creado
     * @throws IllegalArgumentException si el rol o el repositorio no existen
     */
    suspend fun grantRepositoryPermission(
        userId: UUID,
        roleId: UUID,
        repositoryId: UUID,
        grantedBy: UUID,
        expiresAt: Instant? = null
    ): UserPermission {
        // Verificar que el rol existe
        val role = permissionRepository.findRoleById(roleId)
            ?: throw IllegalArgumentException("Role with ID '$roleId' not found")
        
        // Verificar que el repositorio existe
        val repository = repositoryRepository.findById(repositoryId)
            ?: throw IllegalArgumentException("ArtifactRegistry with ID '$repositoryId' not found")
        
        val userPermission = UserPermission.createRepositoryPermission(
            userId = userId,
            roleId = roleId,
            repositoryId = repositoryId,
            grantedBy = grantedBy,
            expiresAt = expiresAt
        )
        
        // Persistir el permiso
        val savedPermission = permissionRepository.saveUserPermission(userPermission)
        
        // Publicar evento de otorgamiento de permiso
        eventPublisher(
            PermissionEvent.PermissionGranted(
                permissionId = savedPermission.id,
                userId = savedPermission.userId,
                roleId = savedPermission.roleId,
                roleName = role.name,
                repositoryId = savedPermission.repositoryId,
                repositoryName = repository?.name,
                grantedBy = savedPermission.grantedBy,
                expiresAt = savedPermission.expiresAt,
                timestamp = savedPermission.grantedAt
            )
        )
        
        return savedPermission
    }
    
    /**
     * Revoca un permiso específico.
     * @param permissionId ID del permiso a revocar
     * @param revokedBy ID del usuario que revoca el permiso
     * @return true si se revocó correctamente, false si no existía
     */
    suspend fun revokePermission(permissionId: UUID, revokedBy: UUID): Boolean {
        // Buscar el permiso por su ID
        val permission = findPermissionById(permissionId)
            ?: return false
        
        val role = permissionRepository.findRoleById(permission.roleId)
            ?: throw IllegalStateException("Role with ID '${permission.roleId}' not found")
        
        val revoked = permissionRepository.revokePermission(permissionId)
        
        if (revoked) {
            // Obtener el nombre del repositorio si existe
            val repositoryName = permission.repositoryId?.let { repoId ->
                repositoryRepository.findById(repoId)?.name
            }
            // Publicar evento de revocación
            eventPublisher(
                PermissionEvent.PermissionRevoked(
                    permissionId = permission.id,
                    userId = permission.userId,
                    roleId = permission.roleId,
                    roleName = role.name,
                    repositoryId = permission.repositoryId,
                    repositoryName = repositoryName,
                    revokedBy = revokedBy,
                    timestamp = Instant.now()
                )
            )
        }
        
        return revoked
    }
    
    /**
     * Busca un permiso por su ID.
     * Implementación temporal que busca en todos los permisos de usuario.
     */
    private suspend fun findPermissionById(permissionId: UUID): UserPermission? {
        // En un sistema real, este método sería parte de la interfaz PermissionRepository
        val allUserIds = findAllUserIds()
        for (userId in allUserIds) {
            val userPermissions = permissionRepository.findPermissionsByUserId(userId)
            val permission = userPermissions.find { it.id == permissionId }
            if (permission != null) {
                return permission
            }
        }
        return null
    }
    
    /**
     * Actualiza la fecha de expiración de un permiso.
     * @param permissionId ID del permiso
     * @param newExpiresAt Nueva fecha de expiración (null para permisos permanentes)
     * @param updatedBy ID del usuario que realiza la actualización
     * @return El permiso actualizado
     * @throws IllegalArgumentException si el permiso no existe
     */
    suspend fun updatePermissionExpiration(
        permissionId: UUID,
        newExpiresAt: Instant?,
        updatedBy: UUID
    ): UserPermission {
        // Buscar el permiso por su ID
        val permission = findPermissionById(permissionId)
            ?: throw IllegalArgumentException("Permission with ID '$permissionId' not found")
        
        // Solo actualizar si hay cambio en la fecha de expiración
        if (permission.expiresAt != newExpiresAt) {
            val updatedPermission = permission.withNewExpiration(newExpiresAt)
            
            val savedPermission = permissionRepository.saveUserPermission(updatedPermission)
            
            // Publicar evento de cambio de expiración
            eventPublisher(
                PermissionEvent.PermissionExpirationChanged(
                    permissionId = savedPermission.id,
                    userId = savedPermission.userId,
                    roleId = savedPermission.roleId,
                    repositoryId = savedPermission.repositoryId,
                    newExpiresAt = savedPermission.expiresAt,
                    updatedBy = updatedBy,
                    timestamp = Instant.now()
                )
            )
            
            return savedPermission
        }
        
        return permission
    }
    
    /**
     * Verifica si un usuario tiene un permiso específico.
     * @param userId ID del usuario
     * @param permission Permiso a verificar
     * @param repositoryId ID del repositorio (opcional, para permisos específicos)
     * @return true si el usuario tiene el permiso, false en caso contrario
     */
    suspend fun hasPermission(
        userId: UUID,
        permission: Permission,
        repositoryId: UUID? = null
    ): Boolean {
        // Buscar permisos activos del usuario
        val userPermissions = permissionRepository.findPermissionsByUserId(userId, activeOnly = true)
        
        // Verificar permisos globales primero
        val hasGlobalPermission = userPermissions
            .filter { it.repositoryId == null } // Solo permisos globales
            .any { userPermission ->
                // Obtener los permisos del rol asociado
                val role = permissionRepository.findRoleById(userPermission.roleId)
                role?.permissions?.contains(permission) == true
            }
        
        if (hasGlobalPermission) {
            return true
        }
        
        // Si se especifica un repositorio, verificar permisos específicos
        if (repositoryId != null) {
            return userPermissions
                .filter { it.repositoryId == null || it.repositoryId == repositoryId }
                .any { userPermission ->
                    // Obtener los permisos del rol asociado
                    val role = permissionRepository.findRoleById(userPermission.roleId)
                    role?.permissions?.contains(permission) == true
                }
        }
        
        return false
    }
    
    /**
     * Obtiene todos los roles disponibles en el sistema.
     * @return Lista de roles
     */
    suspend fun getAllRoles(): List<Role> {
        return permissionRepository.findAllRoles()
    }
    
    /**
     * Busca permisos de usuario según criterios.
     * @param userId ID del usuario (opcional)
     * @param repositoryId ID del repositorio (opcional)
     * @param roleId ID del rol (opcional)
     * @param activeOnly Si solo se deben incluir permisos activos
     * @return Lista de permisos que coinciden con los criterios
     */
    suspend fun findUserPermissions(
        userId: UUID? = null,
        repositoryId: UUID? = null,
        roleId: UUID? = null,
        activeOnly: Boolean = true
    ): List<UserPermission> {
        val result = mutableListOf<UserPermission>()
        
        // Si tenemos un userId específico, buscamos sus permisos
        if (userId != null) {
            val userPermissions = permissionRepository.findPermissionsByUserId(userId, activeOnly)
            result.addAll(userPermissions.filter { permission ->
                (repositoryId == null || permission.repositoryId == repositoryId) &&
                (roleId == null || permission.roleId == roleId)
            })
        } 
        // Si tenemos un repositoryId específico, buscamos permisos para ese repositorio
        else if (repositoryId != null) {
            val repoPermissions = permissionRepository.findPermissionsByRepositoryId(repositoryId, activeOnly)
            result.addAll(repoPermissions.filter { permission ->
                (roleId == null || permission.roleId == roleId)
            })
        }
        // Si solo tenemos roleId o ningún filtro, tenemos que iterar por todos los usuarios
        else {
            val allUserIds = findAllUserIds()
            for (uId in allUserIds) {
                val userPermissions = permissionRepository.findPermissionsByUserId(uId, activeOnly)
                result.addAll(userPermissions.filter { permission ->
                    (roleId == null || permission.roleId == roleId)
                })
            }
        }
        
        return result
    }
}