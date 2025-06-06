package dev.rubentxu.hodei.packages.domain.artifactmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.MerkleRootHash
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactPublishedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDownloadedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactMetadataUpdatedEvent
import dev.rubentxu.hodei.packages.domain.artifactmanagement.events.ArtifactDeletedEvent
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository


import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con la gestión de artefactos.
 * Este servicio coordina operaciones entre artefactos y repositorios, y emite eventos de dominio.
 */
class ArtifactService(
    private val artifactRepository: ArtifactRepository,
    private val registryRepository: RegistryRepository,
    private val publishArtifactEvent: (ArtifactPublishedEvent) -> Unit,
    private val publishDownloadEvent: (ArtifactDownloadedEvent) -> Unit,
    private val publishMetadataUpdateEvent: (ArtifactMetadataUpdatedEvent) -> Unit,
    private val publishDeleteEvent: (ArtifactDeletedEvent) -> Unit
) {
    /**
     * Publica un nuevo artefacto en el sistema.
     * @param group Grupo del artefacto
     * @param name Nombre del artefacto
     * @param version Versión del artefacto
     * @param createdBy ID del usuario que publica el artefacto
     * @param merkleRoot Hash de la raíz Merkle del contenido (opcional)
     * @return El artefacto creado y almacenado
     * @throws IllegalStateException si ya existe un artefacto con las mismas coordenadas
     */
    suspend fun publishArtifact(
        registryId: UUID, // Este UUID se refiere al ID de un Registry, no a RepositoryId directamente.
        group: String,
        name: String,
        version: String,
        createdBy: UserId,
        merkleRoot: String? = null
    ): Result<Artifact> {
        // Validar existencia y estado del Registry (antes era Repository)
        // registryRepository.getRepositoryById(registryId) // Esta función devuelve Registry, no RepositoryId
        //    ?: throw IllegalArgumentException("Registry with ID '$registryId' not found")
        // if (!registryRepository.isRepositoryActive(registryId)) {
        //    throw IllegalStateException("Registry '$registryId' is not active.")
        // }
        // TODO: La lógica de validación del registryId (UUID) necesita ajustarse. 
        // RegistryRepository usa UUID para sus operaciones findById, etc. RepositoryId era un value object.
        // Por ahora, asumo que registryId es el UUID del Registry y las funciones del repo lo aceptan.
        val registry = registryRepository.findById(registryId).getOrNull() // Asumiendo que findById devuelve Result<Registry?>
            ?: throw IllegalArgumentException("Registry with ID '$registryId' not found")
        
        // Asumimos que el Registry tiene una propiedad para saber si está activo, o el servicio tiene un método
        // if (!registry.isActive) { // Suponiendo una propiedad 'isActive' en Registry
        //     throw IllegalStateException("Registry '${registry.name}' is not active.")
        // }
        // O si RegistryRepository tiene un método isRepositoryActive que toma UUID:
        if (!registryRepository.isRepositoryActive(registryId).getOrNull()!!) { // Asumiendo que devuelve Result<Boolean>
             throw IllegalStateException("Registry '$registryId' is not active.")
        }

        val coordinates = ArtifactCoordinates(group, name, version)
        
        val existingArtifactResult = artifactRepository.findByCoordinates(coordinates)
        if (existingArtifactResult.isFailure) {
            return Result.failure(existingArtifactResult.exceptionOrNull()!!)
        }
        if (existingArtifactResult.getOrNull() != null) {
            return Result.failure(IllegalStateException("Artifact ${coordinates.group}:${coordinates.name}:${coordinates.version} already exists"))
        }
        
        val now = Instant.now()
        val newArtifactId = ArtifactId(java.util.UUID.randomUUID().toString())
        val artifact = Artifact(
            id = newArtifactId,
            coordinates = coordinates,
            createdBy = createdBy,
            createdAt = now,
            merkleRoot = merkleRoot?.let { MerkleRootHash(it) }
        )
        
        val savedArtifactResult = artifactRepository.save(artifact)
        if (savedArtifactResult.isFailure) {
            return Result.failure(savedArtifactResult.exceptionOrNull()!!)
        }
        val savedArtifact = savedArtifactResult.getOrNull()!!
        
        publishArtifactEvent(
            ArtifactPublishedEvent(
                artifactId = savedArtifact.id,
                publishedAt = now,
                publishedBy = createdBy
            )
        )
        
        return Result.success(savedArtifact)
    }
    
    suspend fun downloadArtifact(
        artifactId: ArtifactId,
        downloadedBy: UserId? = null,
        clientIp: String? = null,
        userAgent: String? = null
    ): Result<Artifact> { // Cambiado a Result<Artifact>
        val artifactResult = artifactRepository.findById(artifactId)
        if (artifactResult.isFailure) {
            return Result.failure(artifactResult.exceptionOrNull()!!)
        }
        val artifact = artifactResult.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found"))
        
        publishDownloadEvent(
            ArtifactDownloadedEvent(
                artifactId = artifact.id,
                downloadedAt = Instant.now(),
                downloadedBy = downloadedBy,
                clientIp = clientIp,
                userAgent = userAgent
            )
        )
        
        return Result.success(artifact)
    }
    
    suspend fun deleteArtifact(artifactId: ArtifactId, deletedBy: UserId): Result<Boolean> { // Cambiado a Result<Boolean>
        val artifactResult = artifactRepository.findById(artifactId)
        if (artifactResult.isFailure) {
            return Result.failure(artifactResult.exceptionOrNull()!!)
        }
        if (artifactResult.getOrNull() == null) {
            return Result.success(false) // Artifact no encontrado, eliminación no necesaria.
        }
        
        val deletedResult = artifactRepository.deleteById(artifactId)
        if (deletedResult.isFailure) {
            return Result.failure(deletedResult.exceptionOrNull()!!)
        }
        val deleted = deletedResult.getOrNull()!!

        if (deleted) {
            publishDeleteEvent(
                ArtifactDeletedEvent(
                    artifactId = artifactId,
                    deletedAt = Instant.now(),
                    deletedBy = deletedBy
                )
            )
        }
        
        return Result.success(deleted)
    }
    
    suspend fun getAllVersions(
        group: String,
        name: String
    ): Result<List<Artifact>> { // Cambiado a Result<List<Artifact>>
        return artifactRepository.findArtifacts(
            groupFilter = group,
            nameFilter = name
        )
    }
    
    suspend fun getArtifact(
        group: String,
        name: String,
        version: String? = null
    ): Result<Artifact?> { // Cambiado a Result<Artifact?>
        val coordinates = if (version != null) {
            ArtifactCoordinates(group, name, version)
        } else {
            val artifactsResult = getAllVersions(group, name)
            if (artifactsResult.isFailure) {
                return Result.failure(artifactsResult.exceptionOrNull()!!)
            }
            return Result.success(artifactsResult.getOrNull()?.maxByOrNull { it.createdAt })
        }
        
        return artifactRepository.findByCoordinates(coordinates)
    }
    
    /*
    suspend fun updateArtifactMetadata(
        artifactId: ArtifactId,
        metadata: Map<String, String>,
        updatedBy: UserId
    ): Result<Artifact> { // Cambiado a Result<Artifact>
        val artifactResult = artifactRepository.findById(artifactId)
        if (artifactResult.isFailure) {
            return Result.failure(artifactResult.exceptionOrNull()!!)
        }
        val artifact = artifactResult.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Artifact with ID '${artifactId.value}' not found"))
        
        // Este método necesita ser adaptado al nuevo modelo una vez que se defina cómo 
        // se manejan los metadatos en el nuevo modelo de Artefacto
        // Por ejemplo, si Artifact tiene un campo de metadatos: 
        // val updatedArtifact = artifact.copy(customProperties = metadata, updatedAt = Instant.now()) // Asumiendo updatedAt también
        // val savedResult = artifactRepository.save(updatedArtifact)
        // if (savedResult.isSuccess) {
        //     publishMetadataUpdateEvent(
        //         ArtifactMetadataUpdatedEvent(
        //             artifactId = updatedArtifact.id,
        //             updatedAt = updatedArtifact.updatedAt!!, // Asumiendo que updatedAt se actualiza
        //             updatedBy = updatedBy,
        //             updatedMetadata = metadata
        //         )
        //     )
        // }
        // return savedResult
        return Result.failure(NotImplementedError("updateArtifactMetadata not implemented for new model"))
    }
    */
} 