package dev.rubentxu.hodei.packages.domain.service

import dev.rubentxu.hodei.packages.domain.events.artifact.ArtifactEvent
import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import dev.rubentxu.hodei.packages.domain.model.repository.Repository
import dev.rubentxu.hodei.packages.domain.model.repository.RepositoryType
import dev.rubentxu.hodei.packages.domain.repository.ArtifactRepository
import dev.rubentxu.hodei.packages.domain.repository.RepositoryRepository
import java.time.Instant
import java.util.UUID

/**
 * Servicio de dominio que encapsula la lógica de negocio relacionada con la gestión de artefactos.
 * Este servicio coordina operaciones entre artefactos y repositorios, y emite eventos de dominio.
 */
class ArtifactService(
    private val artifactRepository: ArtifactRepository,
    private val repositoryRepository: RepositoryRepository,
    private val eventPublisher: (ArtifactEvent) -> Unit
) {
    /**
     * Sube un nuevo artefacto a un repositorio.
     * @param repositoryId ID del repositorio donde se subirá el artefacto
     * @param groupId Grupo del artefacto
     * @param artifactId ID del artefacto
     * @param version Versión del artefacto
     * @param fileSize Tamaño del archivo en bytes
     * @param sha256 Hash SHA-256 del contenido del artefacto
     * @param metadata Metadatos adicionales del artefacto
     * @param uploadedBy ID del usuario que sube el artefacto
     * @return El artefacto creado y almacenado
     * @throws IllegalArgumentException si el repositorio no existe
     * @throws IllegalStateException si ya existe un artefacto con las mismas coordenadas
     */
    suspend fun uploadArtifact(
        repositoryId: UUID,
        groupId: String,
        artifactId: String,
        version: String,
        fileSize: Long,
        sha256: String,
        metadata: Map<String, String>,
        uploadedBy: UUID
    ): Artifact {
        // Verificar que el repositorio existe
        val repository = repositoryRepository.findById(repositoryId)
            ?: throw IllegalArgumentException("Repository with ID '$repositoryId' not found")
        
        // Verificar si ya existe un artefacto con las mismas coordenadas
        val existingArtifact = artifactRepository.findByCoordinates(
            repositoryId = repositoryId,
            groupId = groupId,
            artifactId = artifactId,
            version = version
        )
        
        if (existingArtifact != null) {
            throw IllegalStateException("Artifact $groupId:$artifactId:$version already exists in repository ${repository.name}")
        }
        
        val now = Instant.now()
        val artifact = Artifact(
            id = UUID.randomUUID(),
            repositoryId = repositoryId,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            repositoryType = repository.type,
            fileSize = fileSize,
            sha256 = sha256,
            createdBy = uploadedBy,
            createdAt = now,
            updatedAt = now,
            metadata = metadata
        )
        
        // Persistir el artefacto
        val savedArtifact = artifactRepository.save(artifact)
        
        // Publicar evento de subida de artefacto
        eventPublisher(
            ArtifactEvent.ArtifactUploaded(
                artifactId = savedArtifact.id,
                repositoryId = savedArtifact.repositoryId,
                groupId = savedArtifact.groupId,
                artifactName = savedArtifact.artifactId,
                version = savedArtifact.version,
                fileSize = savedArtifact.fileSize,
                uploadedBy = uploadedBy,
                timestamp = now
            )
        )
        
        return savedArtifact
    }
    
    /**
     * Registra la descarga de un artefacto y emite un evento.
     * @param artifactId ID del artefacto descargado
     * @param downloadedBy ID del usuario que descarga (null para descargas anónimas)
     * @param clientIp Dirección IP del cliente (opcional)
     * @param userAgent User-Agent del cliente (opcional)
     * @return El artefacto descargado
     * @throws IllegalArgumentException si el artefacto no existe
     */
    suspend fun downloadArtifact(
        artifactId: UUID,
        downloadedBy: UUID? = null,
        clientIp: String? = null,
        userAgent: String? = null
    ): Artifact {
        val artifact = artifactRepository.findById(artifactId)
            ?: throw IllegalArgumentException("Artifact with ID '$artifactId' not found")
        
        // Publicar evento de descarga
        eventPublisher(
            ArtifactEvent.ArtifactDownloaded(
                artifactId = artifact.id,
                repositoryId = artifact.repositoryId,
                groupId = artifact.groupId,
                artifactName = artifact.artifactId,
                version = artifact.version,
                downloadedBy = downloadedBy,
                timestamp = Instant.now(),
                clientIp = clientIp,
                userAgent = userAgent
            )
        )
        
        return artifact
    }
    
    /**
     * Actualiza los metadatos de un artefacto existente.
     * @param artifactId ID del artefacto a actualizar
     * @param metadata Nuevos metadatos a aplicar (se fusionarán con los existentes)
     * @param updatedBy ID del usuario que realiza la actualización
     * @return El artefacto actualizado
     * @throws IllegalArgumentException si el artefacto no existe
     */
    suspend fun updateArtifactMetadata(
        artifactId: UUID,
        metadata: Map<String, String>,
        updatedBy: UUID
    ): Artifact {
        val artifact = artifactRepository.findById(artifactId)
            ?: throw IllegalArgumentException("Artifact with ID '$artifactId' not found")
        
        // Fusionar metadatos existentes con los nuevos
        val updatedMetadata = artifact.metadata + metadata
        
        // Solo actualizar si hay cambios en los metadatos
        if (updatedMetadata != artifact.metadata) {
            val updatedArtifact = artifact.copy(
                metadata = updatedMetadata,
                updatedAt = Instant.now()
            )
            
            val savedArtifact = artifactRepository.save(updatedArtifact)
            
            // Publicar evento de actualización de metadatos
            eventPublisher(
                ArtifactEvent.ArtifactMetadataUpdated(
                    artifactId = savedArtifact.id,
                    repositoryId = savedArtifact.repositoryId,
                    groupId = savedArtifact.groupId,
                    artifactName = savedArtifact.artifactId,
                    version = savedArtifact.version,
                    updatedBy = updatedBy,
                    timestamp = savedArtifact.updatedAt,
                    updatedMetadata = metadata
                )
            )
            
            return savedArtifact
        }
        
        return artifact
    }
    
    /**
     * Elimina un artefacto del sistema.
     * @param artifactId ID del artefacto a eliminar
     * @param deletedBy ID del usuario que realiza la eliminación
     * @return true si se eliminó correctamente, false si no existía
     */
    suspend fun deleteArtifact(artifactId: UUID, deletedBy: UUID): Boolean {
        val artifact = artifactRepository.findById(artifactId)
            ?: return false
        
        val deleted = artifactRepository.deleteById(artifactId)
        
        if (deleted) {
            // Publicar evento de eliminación
            eventPublisher(
                ArtifactEvent.ArtifactDeleted(
                    artifactId = artifact.id,
                    repositoryId = artifact.repositoryId,
                    groupId = artifact.groupId,
                    artifactName = artifact.artifactId,
                    version = artifact.version,
                    deletedBy = deletedBy,
                    timestamp = Instant.now()
                )
            )
        }
        
        return deleted
    }
    
    /**
     * Busca artefactos en un repositorio específico.
     * @param repositoryId ID del repositorio
     * @return Lista de artefactos en el repositorio
     * @throws IllegalArgumentException si el repositorio no existe
     */
    suspend fun findArtifactsByRepository(repositoryId: UUID): List<Artifact> {
        // Verificar que el repositorio existe
        val repository = repositoryRepository.findById(repositoryId)
            ?: throw IllegalArgumentException("Repository with ID '$repositoryId' not found")
        
        return artifactRepository.findByRepositoryId(repositoryId)
    }
    
    /**
     * Busca todas las versiones de un artefacto específico.
     * @param repositoryId ID del repositorio
     * @param groupId Grupo del artefacto
     * @param artifactId ID del artefacto
     * @return Lista de versiones del artefacto ordenadas por fecha
     * @throws IllegalArgumentException si el repositorio no existe
     */
    suspend fun findArtifactVersions(
        repositoryId: UUID,
        groupId: String,
        artifactId: String
    ): List<Artifact> {
        // Verificar que el repositorio existe
        val repository = repositoryRepository.findById(repositoryId)
            ?: throw IllegalArgumentException("Repository with ID '$repositoryId' not found")
        
        return artifactRepository.findAllVersions(
            repositoryId = repositoryId,
            groupId = groupId,
            artifactId = artifactId
        )
    }
    
    /**
     * Obtiene un artefacto específico por sus coordenadas.
     * @param repositoryId ID del repositorio
     * @param groupId Grupo del artefacto
     * @param artifactId ID del artefacto
     * @param version Versión específica (opcional, si no se especifica se devuelve la última)
     * @return El artefacto si existe, null en caso contrario
     * @throws IllegalArgumentException si el repositorio no existe
     */
    suspend fun getArtifact(
        repositoryId: UUID,
        groupId: String,
        artifactId: String,
        version: String? = null
    ): Artifact? {
        // Verificar que el repositorio existe
        val repository = repositoryRepository.findById(repositoryId)
            ?: throw IllegalArgumentException("Repository with ID '$repositoryId' not found")
        
        return artifactRepository.findByCoordinates(
            repositoryId = repositoryId,
            groupId = groupId,
            artifactId = artifactId,
            version = version
        )
    }
}