package dev.rubentxu.hodei.packages.domain.artifactmanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.time.Instant

data class ArtifactMetadataUpdatedEvent(
    val artifactId: ArtifactId,
    val updatedAt: Instant,
    val updatedBy: UserId,
    val updatedMetadata: Map<String, String>
) 

