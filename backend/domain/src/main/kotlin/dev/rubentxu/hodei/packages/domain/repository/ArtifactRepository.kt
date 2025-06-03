package dev.rubentxu.hodei.packages.domain.repository

import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import java.util.UUID

/**
 * Puerto (interfaz) para la persistencia de artefactos.
 * Define operaciones CRUD y de búsqueda para artefactos dentro de repositorios.
 */
interface ArtifactRepository {
    /**
     * Guarda un nuevo artefacto o actualiza uno existente.
     * @param artifact El artefacto a guardar
     * @return El artefacto guardado con posibles modificaciones
     */
    suspend fun save(artifact: Artifact): Artifact
    
    /**
     * Busca un artefacto por su ID.
     * @param id ID del artefacto
     * @return El artefacto si existe, null en caso contrario
     */
    suspend fun findById(id: UUID): Artifact?
    
    /**
     * Busca artefactos por su repositorio.
     * @param repositoryId ID del repositorio
     * @return Lista de artefactos en el repositorio
     */
    suspend fun findByRepositoryId(repositoryId: UUID): List<Artifact>
    
    /**
     * Busca un artefacto específico en un repositorio por sus coordenadas.
     * @param repositoryId ID del repositorio
     * @param groupId Grupo del artefacto
     * @param artifactId ID del artefacto
     * @param version Versión del artefacto (opcional, si no se especifica se devuelve la última)
     * @return El artefacto si existe, null en caso contrario
     */
    suspend fun findByCoordinates(
        repositoryId: UUID,
        groupId: String,
        artifactId: String,
        version: String? = null
    ): Artifact?
    
    /**
     * Busca todas las versiones de un artefacto en un repositorio.
     * @param repositoryId ID del repositorio
     * @param groupId Grupo del artefacto
     * @param artifactId ID del artefacto
     * @return Lista de versiones del artefacto ordenadas por fecha (más reciente primero)
     */
    suspend fun findAllVersions(
        repositoryId: UUID,
        groupId: String,
        artifactId: String
    ): List<Artifact>
    
    /**
     * Elimina un artefacto por su ID.
     * @param id ID del artefacto a eliminar
     * @return true si se eliminó correctamente, false si no existía
     */
    suspend fun deleteById(id: UUID): Boolean
    
    /**
     * Elimina todos los artefactos de un repositorio.
     * @param repositoryId ID del repositorio
     * @return Número de artefactos eliminados
     */
    suspend fun deleteByRepositoryId(repositoryId: UUID): Int
}