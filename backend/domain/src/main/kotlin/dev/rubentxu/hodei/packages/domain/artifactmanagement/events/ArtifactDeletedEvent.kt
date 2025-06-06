package dev.rubentxu.hodei.packages.domain.artifactmanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import java.time.Instant

data class ArtifactDeletedEvent(
    val artifactId: ArtifactId,
    val deletedAt: Instant,
    val deletedBy: UserId
) 

