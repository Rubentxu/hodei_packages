package dev.rubentxu.hodei.packages.application.artifact

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId


interface ArtifactPublicationService {
    suspend fun publish(command: PublishArtifactCommand): Result<Artifact>
}

data class PublishArtifactCommand(
    val coordinates: ArtifactCoordinates,
    val fileContent: ByteArray,
    val createdBy: UserId
)
