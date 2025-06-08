package dev.rubentxu.hodei.packages.domain.artifactmanagement.service

import dev.rubentxu.hodei.packages.domain.artifactmanagement.command.UploadArtifactCommand
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId

/**
 * Interface for artifact management operations, as exposed in the domain diagrams.
 */
interface ArtifactServicePort {
    suspend fun uploadArtifact(command: UploadArtifactCommand): Result<Artifact>
    suspend fun downloadArtifact(
        artifactId: ArtifactId,
        downloadedBy: UserId? = null,
        clientIp: String? = null,
        userAgent: String? = null
    ): Result<Artifact>
    suspend fun getArtifact(artifactCoordinates: ArtifactCoordinates): Result<Artifact?>
    suspend fun getAllVersions(group: String, name: String): Result<List<Artifact>>
    suspend fun updateArtifactMetadata(
        artifactId: ArtifactId,
        newMetadataValues: ArtifactMetadata,
        updatedBy: UserId
    ): Result<Artifact>
    suspend fun deleteArtifact(artifactId: ArtifactId, deletedBy: UserId): Result<Boolean>
    suspend fun retrieveArtifactContent(registryId: RegistryId, contentHash: ContentHash): Result<ByteArray>
    suspend fun generateArtifactDescriptor(artifactId: ArtifactId, artifactType: ArtifactType): Result<String>
}
