package dev.rubentxu.hodei.packages.domain.model.artifact

import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import java.time.Instant
import java.util.UUID

/**
 * Representa un artefacto almacenado en un repositorio.
 * Un artefacto es un paquete de software con metadatos específicos según su tipo (Maven, NPM).
 */
data class Artifact(
    val id: UUID,
    val repositoryId: UUID,
    val groupId: String,
    val artifactId: String,
    val version: String,
    val repositoryType: RepositoryType,
    val fileSize: Long,
    val sha256: String,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        validate()
    }
    
    private fun validate() {
        require(groupId.isNotBlank()) { "Group ID cannot be blank" }
        require(artifactId.isNotBlank()) { "Artifact ID cannot be blank" }
        require(isValidVersion(version)) { "Version must follow semantic versioning format" }
        require(fileSize >= 0) { "File size cannot be negative" }
        require(isValidSha256(sha256)) { "SHA-256 hash must be a valid 64-character hexadecimal string" }
    }
    
    private fun isValidVersion(version: String): Boolean {
        // Implementación básica de validación de versión semántica
        // Permite formatos como: 1.0.0, 1.0.0-alpha.1, 1.0.0-beta+exp.sha.5114f85
        val semverRegex = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$")
        return semverRegex.matches(version)
    }
    
    private fun isValidSha256(hash: String): Boolean {
        // El hash SHA-256 debe ser una cadena hexadecimal de 64 caracteres
        val sha256Regex = Regex("^[a-fA-F0-9]{64}$")
        return sha256Regex.matches(hash)
    }
    
    /**
     * Obtiene una representación única del artefacto como coordenadas.
     * @return String con el formato "groupId:artifactId:version"
     */
    fun getCoordinates(): String {
        return "$groupId:$artifactId:$version"
    }
    
    companion object {
        /**
         * Crea un nuevo artefacto con una nueva versión basada en el artefacto original.
         */
        fun createNewVersion(
            original: Artifact,
            newVersion: String,
            newFileSize: Long,
            newSha256: String,
            updatedMetadata: Map<String, String> = original.metadata
        ): Artifact {
            return Artifact(
                id = UUID.randomUUID(),
                repositoryId = original.repositoryId,
                groupId = original.groupId,
                artifactId = original.artifactId,
                version = newVersion,
                repositoryType = original.repositoryType,
                fileSize = newFileSize,
                sha256 = newSha256,
                createdBy = original.createdBy,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                metadata = updatedMetadata
            )
        }
    }
}