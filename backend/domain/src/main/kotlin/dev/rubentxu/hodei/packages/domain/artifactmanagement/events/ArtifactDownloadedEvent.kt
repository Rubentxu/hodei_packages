package dev.rubentxu.hodei.packages.domain.artifactmanagement.events

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.time.Instant

data class ArtifactDownloadedEvent(
    val artifactId: ArtifactId,
    val downloadedAt: Instant,
    val downloadedBy: UserId? = null,
    val clientIp: String? = null,
    val userAgent: String? = null
) 