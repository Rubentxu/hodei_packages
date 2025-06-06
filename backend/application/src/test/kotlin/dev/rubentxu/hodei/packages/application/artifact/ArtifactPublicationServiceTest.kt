package dev.rubentxu.hodei.packages.application.artifact

import dev.rubentxu.hodei.packages.application.artifactmanagement.service.ArtifactPublicationService
import dev.rubentxu.hodei.packages.application.artifactmanagement.service.PublishArtifactCommand
import dev.rubentxu.hodei.packages.domain.model.artifact.Artifact
import dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.model.artifact.UserId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ArtifactPublicationServiceTest : BehaviorSpec({
    given("un comando de publicación de artefacto válido") {
        val command = PublishArtifactCommand(
            coordinates = ArtifactCoordinates("dev.rubentxu", "libfoo", "1.0.0"),
            fileContent = "contenido".toByteArray(),
            createdBy = UserId("dev")
        )
        val fakeService = object : ArtifactPublicationService {
            override suspend fun publish(command: PublishArtifactCommand): Result<Artifact> {
                val artifact = Artifact(
                    id = dev.rubentxu.hodei.packages.domain.model.artifact.ArtifactId("artifact-1"),
                    coordinates = command.coordinates,
                    createdBy = command.createdBy,
                    createdAt = java.time.Instant.now()
                )
                return Result.success(artifact)
            }
        }

        `when`("se publica el artefacto") {
            val result = fakeService.publish(command)
            then("el resultado debe ser exitoso y contener el artefacto publicado") {
                result.isSuccess shouldBe true
                result.getOrNull()?.coordinates shouldBe command.coordinates
                result.getOrNull()?.createdBy shouldBe command.createdBy
            }
        }
    }
})
