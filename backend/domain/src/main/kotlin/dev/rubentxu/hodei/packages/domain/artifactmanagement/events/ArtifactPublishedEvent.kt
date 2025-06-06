package dev.rubentxu.hodei.packages.domain.artifactmanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import java.time.Instant

data class ArtifactPublishedEvent(
    val artifactId: ArtifactId,
    val publishedAt: Instant,
    val publishedBy: UserId
) 

