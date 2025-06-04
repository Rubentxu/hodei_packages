package dev.rubentxu.hodei.packages.domain.repository


import dev.rubentxu.hodei.packages.domain.model.registry.ArtifactRegistry
import dev.rubentxu.hodei.packages.domain.model.registry.RegistryType

import java.util.UUID

/**
 * Puerto (interfaz) para la persistencia de repositorios de artefactos.
 * Define operaciones CRUD y de búsqueda para repositorios.
 */
interface RepositoryRepository {
    /**
     * Guarda un nuevo repositorio o actualiza uno existente.
     * @param registry El repositorio a guardar
     * @return El repositorio guardado con posibles modificaciones (ej. ID generado)
     */
    suspend fun save(registry: ArtifactRegistry): ArtifactRegistry
    
    /**
     * Busca un repositorio por su ID.
     * @param id ID del repositorio
     * @return El repositorio si existe, null en caso contrario
     */
    suspend fun findById(id: UUID): ArtifactRegistry?
    
    /**
     * Busca un repositorio por su nombre.
     * @param name Nombre del repositorio
     * @return El repositorio si existe, null en caso contrario
     */
    suspend fun findByName(name: String): ArtifactRegistry?
    
    /**
     * Obtiene todos los repositorios del sistema.
     * @param type Tipo opcional de repositorio para filtrar (MAVEN, NPM)
     * @return Lista de repositorios
     */
    suspend fun findAll(type: RegistryType? = null): List<ArtifactRegistry>
    
    /**
     * Verifica si existe un repositorio con el nombre dado.
     * @param name Nombre del repositorio
     * @return true si existe, false en caso contrario
     */
    suspend fun existsByName(name: String): Boolean
    
    /**
     * Elimina un repositorio por su ID.
     * @param id ID del repositorio a eliminar
     * @return true si se eliminó correctamente, false si no existía
     */
    suspend fun deleteById(id: UUID): Boolean
}