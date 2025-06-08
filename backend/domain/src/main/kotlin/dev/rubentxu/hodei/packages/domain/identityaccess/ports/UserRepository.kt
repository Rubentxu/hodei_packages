package dev.rubentxu.hodei.packages.domain.identityaccess.ports

import dev.rubentxu.hodei.packages.domain.identityaccess.model.User
import java.util.UUID

/**
 * Repository interface for managing {@link AdminUser} entities.
 */
interface UserRepository {
    /**
     * Saves an admin user.
     * @param user The admin user to save.
     * @return The saved admin user.
     */
    fun save(user: User): User

    /**
     * Finds an admin user by ID.
     * @param id The ID to search for.
     * @return The found admin user, or null if not found.
     */
    fun findById(id: UUID): User?

    /**
     * Finds an admin user by username.
     * @param username The username to search for.
     * @return The found admin user, or null if not found.
     */
    fun findByUsername(username: String): User?

    /**
     * Finds an admin user by email.
     * @param email The email to search for.
     * @return The found admin user, or null if not found.
     */
    fun findByEmail(email: String): User?

    /**
     * Finds all admin users.
     * @return A list of all admin users.
     */
    fun findAll(): List<User>

    /**
     * Deletes an admin user by ID.
     * @param id The ID of the admin user to delete.
     */
    fun delete(id: UUID)

    /**
     * Checks if there is already an admin user in the system.
     * @return true if an admin user exists, false otherwise.
     */
    fun existsAdmin(): Boolean = findAll().any { it.isActive }
}