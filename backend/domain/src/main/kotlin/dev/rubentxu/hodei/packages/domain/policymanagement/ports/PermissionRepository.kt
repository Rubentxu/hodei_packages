package dev.rubentxu.hodei.packages.domain.policymanagement.ports

import dev.rubentxu.hodei.packages.domain.model.permission.Role
import dev.rubentxu.hodei.packages.domain.model.permission.UserPermission
import java.util.UUID

/**
 * Port interface for the persistence and querying of user roles and permissions.
 * Defines operations for managing the RBAC system.
 */
interface PermissionRepository {
    /**
     * Saves a role in the system.
     * @param role The role to save
     * @return Result with the saved role or error
     */
    suspend fun saveRole(role: Role): Result<Role>

    /**
     * Finds a role by its ID.
     * @param id Role ID
     * @return Result with the role if it exists, or error if not found
     */
    suspend fun findRoleById(id: UUID): Result<Role>

    /**
     * Finds a role by its name.
     * @param name Role name
     * @return Result with the role if it exists, or error if not found
     */
    suspend fun findRoleByName(name: String): Result<Role>

    /**
     * Gets all roles in the system.
     * @param systemRolesOnly If true, returns only predefined roles
     * @return Result with the list of roles or error
     */
    suspend fun findAllRoles(systemRolesOnly: Boolean = false): Result<List<Role>>

    /**
     * Deletes a role by its ID.
     * @param id ID of the role to delete
     * @return Result with true if successfully deleted, false if it didn't exist or is a system role, or error
     */
    suspend fun deleteRoleById(id: UUID): Result<Boolean>

    /**
     * Saves a user permission.
     * @param userPermission The permission to save
     * @return Result with the saved permission or error
     */
    suspend fun saveUserPermission(userPermission: UserPermission): Result<UserPermission>

    /**
     * Finds permissions by user.
     * @param userId User ID
     * @param activeOnly If true, returns only active (non-expired) permissions
     * @return Result with the list of user permissions or error
     */
    suspend fun findPermissionsByUserId(userId: UUID, activeOnly: Boolean = true): Result<List<UserPermission>>

    /**
     * Finds permissions by repository.
     * @param repositoryId Repository ID
     * @param activeOnly If true, returns only active (non-expired) permissions
     * @return Result with the list of permissions for the repository or error
     */
    suspend fun findPermissionsByRepositoryId(
        repositoryId: UUID,
        activeOnly: Boolean = true
    ): Result<List<UserPermission>>

    /**
     * Finds specific permissions for a user in a repository.
     * @param userId User ID
     * @param repositoryId Repository ID
     * @param activeOnly If true, returns only active (non-expired) permissions
     * @return Result with the list of user permissions for the repository or error
     */
    suspend fun findUserPermissionsForRepository(
        userId: UUID,
        repositoryId: UUID,
        activeOnly: Boolean = true
    ): Result<List<UserPermission>>

    /**
     * Revokes (deletes) a user permission.
     * @param id ID of the permission to revoke
     * @return Result with true if successfully revoked, false if it didn't exist, or error
     */
    suspend fun revokePermission(id: UUID): Result<Boolean>

    /**
     * Revokes all permissions of a user for a repository.
     * @param userId User ID
     * @param repositoryId Repository ID
     * @return Result with the number of permissions revoked or error
     */
    suspend fun revokeAllUserPermissionsForRepository(userId: UUID, repositoryId: UUID): Result<Int>
}