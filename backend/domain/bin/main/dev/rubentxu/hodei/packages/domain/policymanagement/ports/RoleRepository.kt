package dev.rubentxu.hodei.packages.domain.policymanagement.ports

import dev.rubentxu.hodei.packages.domain.model.permission.Role
import java.util.UUID

/**
 * Interface for managing the persistence of Role entities.
 */
interface RoleRepository {

    /**
     * Finds a role by its unique identifier.
     * @param id The UUID of the role.
     * @return Result with the Role if found, or error.
     */
    suspend fun findById(id: UUID): Result<Role?>

    /**
     * Finds a role by its name.
     * @param name The name of the role.
     * @return Result with the Role if found, or error.
     */
    suspend fun findByName(name: String): Result<Role?>

    /**
     * Retrieves all roles.
     * @return Result with a list of all roles, or error.
     */
    suspend fun findAll(): Result<List<Role>>

    /**
     * Saves a role (either creates a new one or updates an existing one).
     * @param role The role to save.
     * @return Result with the saved role, or error.
     */
    suspend fun save(role: Role): Result<Role>

    /**
     * Deletes a role by its unique identifier.
     * @param id The UUID of the role to delete.
     * @return Result with true if successfully deleted, false if it didn't exist, or error
     */
    suspend fun delete(id: UUID): Result<Boolean>

    /**
     * Finds all system roles.
     * @return Result with a list of system roles, or error.
     */
    suspend fun findSystemRoles(): Result<List<Role>>
}