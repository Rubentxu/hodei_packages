package dev.rubentxu.hodei.packages.domain.repository

import dev.rubentxu.hodei.packages.domain.model.permission.Role
import java.util.UUID

/**
 * Interface for managing the persistence of Role entities.
 */
interface RoleRepository {

    /**
     * Finds a role by its unique identifier.
     * @param id The UUID of the role.
     * @return The Role if found, null otherwise.
     */
    suspend fun findById(id: UUID): Role?

    /**
     * Finds a role by its name.
     * @param name The name of the role.
     * @return The Role if found, null otherwise.
     */
    suspend fun findByName(name: String): Role?

    /**
     * Retrieves all roles.
     * @return A list of all roles.
     */
    suspend fun findAll(): List<Role>

    /**
     * Saves a role (either creates a new one or updates an existing one).
     * @param role The role to save.
     * @return The saved role.
     */
    suspend fun save(role: Role): Role

    /**
     * Deletes a role by its unique identifier.
     * @param id The UUID of the role to delete.
     */
    suspend fun delete(id: UUID)

    /**
     * Finds all system roles.
     * @return A list of system roles.
     */
    suspend fun findSystemRoles(): List<Role>
}
