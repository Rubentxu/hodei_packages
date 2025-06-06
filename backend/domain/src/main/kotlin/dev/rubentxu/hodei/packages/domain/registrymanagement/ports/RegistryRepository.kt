package dev.rubentxu.hodei.packages.domain.registrymanagement.ports

import dev.rubentxu.hodei.packages.domain.registrymanagement.model.Registry
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryType
import java.util.*


/**
 * Port for obtaining information about repositories that store artifacts.
 * This port represents the abstraction that allows the domain to access repository information
 * without depending on storage or persistence details.
 */
interface RegistryRepository {
    /**
     * Saves a new registry or updates an existing one.
     * @param registry The registry to save.
     * @return Result with the saved registry (possibly modified with generated ID) or error.
     */
    suspend fun save(registry: Registry): Result<Registry>

    /**
     * Finds a registry by its ID.
     * @param id Registry ID.
     * @return Result with the registry if found, or null if not found, or an error.
     */
    suspend fun findById(id: UUID): Result<Registry?>

    /**
     * Finds a registry by its name.
     * @param name Registry name.
     * @return Result with the registry if found, or null if not found, or an error.
     */
    suspend fun findByName(name: String): Result<Registry?>

    /**
     * Gets all registries in the system.
     * @param type Optional registry type to filter by (MAVEN, NPM).
     * @return Result with list of registries or error.
     */
    suspend fun findAll(type: RegistryType? = null): Result<List<Registry>>

    /**
     * Checks if a registry with the given name exists.
     * @param name Registry name.
     * @return boolean indicating existence. Throws exception on underlying access error.
     */
    suspend fun existsByName(name: String): Boolean

    /**
     * Deletes a registry by its ID.
     * @param id ID of the registry to delete.
     * @return true if successfully deleted, false if it didn't exist. Throws exception on underlying access error.
     */
    suspend fun deleteById(id: UUID): Boolean

    /**
     * Gets a registry by its ID.
     * @param id ID of the registry to search for.
     * @return Result with the found registry, or an error if not found or another error occurs.
     */
    suspend fun getRepositoryById(id: UUID): Result<Registry> // Asumo que esta se mantiene con Result por si devuelve Registry o error de no encontrado

    /**
     * Checks if a registry is active and available.
     * @param id ID of the registry to check.
     * @return true if the registry is active, false otherwise. Throws exception on underlying access error.
     */
    suspend fun isRepositoryActive(id: UUID): Boolean

}
