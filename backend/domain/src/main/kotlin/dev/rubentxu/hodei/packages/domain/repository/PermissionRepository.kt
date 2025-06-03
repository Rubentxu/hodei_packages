package dev.rubentxu.hodei.packages.domain.repository

import dev.rubentxu.hodei.packages.domain.model.permission.Role
import dev.rubentxu.hodei.packages.domain.model.permission.UserPermission
import java.util.UUID

/**
 * Puerto (interfaz) para la persistencia y consulta de roles y permisos de usuario.
 * Define operaciones para la gestión del sistema RBAC.
 */
interface PermissionRepository {
    /**
     * Guarda un rol en el sistema.
     * @param role El rol a guardar
     * @return El rol guardado con posibles modificaciones
     */
    suspend fun saveRole(role: Role): Role
    
    /**
     * Busca un rol por su ID.
     * @param id ID del rol
     * @return El rol si existe, null en caso contrario
     */
    suspend fun findRoleById(id: UUID): Role?
    
    /**
     * Busca un rol por su nombre.
     * @param name Nombre del rol
     * @return El rol si existe, null en caso contrario
     */
    suspend fun findRoleByName(name: String): Role?
    
    /**
     * Obtiene todos los roles del sistema.
     * @param systemRolesOnly Si es true, devuelve solo roles predefinidos
     * @return Lista de roles
     */
    suspend fun findAllRoles(systemRolesOnly: Boolean = false): List<Role>
    
    /**
     * Elimina un rol por su ID.
     * @param id ID del rol a eliminar
     * @return true si se eliminó correctamente, false si no existía o es un rol de sistema
     */
    suspend fun deleteRoleById(id: UUID): Boolean
    
    /**
     * Guarda un permiso de usuario.
     * @param userPermission El permiso a guardar
     * @return El permiso guardado con posibles modificaciones
     */
    suspend fun saveUserPermission(userPermission: UserPermission): UserPermission
    
    /**
     * Busca permisos por usuario.
     * @param userId ID del usuario
     * @param activeOnly Si es true, devuelve solo permisos activos (no expirados)
     * @return Lista de permisos del usuario
     */
    suspend fun findPermissionsByUserId(userId: UUID, activeOnly: Boolean = true): List<UserPermission>
    
    /**
     * Busca permisos por repositorio.
     * @param repositoryId ID del repositorio
     * @param activeOnly Si es true, devuelve solo permisos activos (no expirados)
     * @return Lista de permisos para el repositorio
     */
    suspend fun findPermissionsByRepositoryId(repositoryId: UUID, activeOnly: Boolean = true): List<UserPermission>
    
    /**
     * Busca permisos específicos para un usuario en un repositorio.
     * @param userId ID del usuario
     * @param repositoryId ID del repositorio
     * @param activeOnly Si es true, devuelve solo permisos activos (no expirados)
     * @return Lista de permisos del usuario para el repositorio
     */
    suspend fun findUserPermissionsForRepository(
        userId: UUID,
        repositoryId: UUID,
        activeOnly: Boolean = true
    ): List<UserPermission>
    
    /**
     * Revoca (elimina) un permiso de usuario.
     * @param id ID del permiso a revocar
     * @return true si se revocó correctamente, false si no existía
     */
    suspend fun revokePermission(id: UUID): Boolean
    
    /**
     * Revoca todos los permisos de un usuario para un repositorio.
     * @param userId ID del usuario
     * @param repositoryId ID del repositorio
     * @return Número de permisos revocados
     */
    suspend fun revokeAllUserPermissionsForRepository(userId: UUID, repositoryId: UUID): Int
}