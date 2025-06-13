package dev.rubentxu.hodei.packages.app.features.packages.routes

import dev.rubentxu.hodei.packages.application.artifactmanagement.service.ArtifactPublicationService
import dev.rubentxu.hodei.packages.application.artifactmanagement.service.PublishArtifactCommand
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactStatus
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

// Consider renaming artifactPublicationService if it's a general ArtifactServicePort
fun Route.mavenRoutes(artifactService: ArtifactPublicationService) { // Renamed parameter for clarity

    authenticate("basicAuth") {
        put("/maven/{repoId}/{groupId}/{artifactId}/{version}/{file}") {
            // ... (existing PUT implementation, unchanged) ...
            val repoId = call.parameters["repoId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                "Repository ID is missing"
            )
            val groupId =
                call.parameters["groupId"] ?: return@put call.respond(HttpStatusCode.BadRequest, "GroupId is missing")
            val artifactIdFromPath = call.parameters["artifactId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                "ArtifactId is missing"
            )
            val versionFromPath =
                call.parameters["version"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Version is missing")
            val fileName =
                call.parameters["file"] ?: return@put call.respond(HttpStatusCode.BadRequest, "File name is missing")

            val principal = call.principal<UserIdPrincipal>()
            if (principal == null) {
                call.application.environment.log.warn("Principal not found in authenticated Maven PUT route.")
                return@put call.respond(HttpStatusCode.Unauthorized, "Authentication failed: User principal not found.")
            }
            val createdBy = UserId(principal.name)

            try {
                val extension = fileName.substringAfterLast('.', "")
                if (extension.isEmpty() || fileName.endsWith(".")) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        "File name missing or invalid extension: $fileName"
                    )
                }
                val nameWithoutExtension = fileName.substringBeforeLast('.')
                val expectedFilePrefix = "$artifactIdFromPath-$versionFromPath"
                var classifier: String? = null
                when {
                    nameWithoutExtension == expectedFilePrefix -> {}
                    nameWithoutExtension.startsWith("$expectedFilePrefix-") -> {
                        classifier = nameWithoutExtension.substringAfter("$expectedFilePrefix-")
                        if (classifier.isEmpty()) {
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                "Invalid empty classifier in file name: $fileName"
                            )
                        }
                    }

                    else -> {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            "File name '$fileName' does not match expected format 'artifactId-version[-classifier].extension' " +
                                    "based on path parameters artifactId '$artifactIdFromPath' and version '$versionFromPath'."
                        )
                    }
                }
                val coordinates = ArtifactCoordinates(
                    repoId = repoId,
                    group = groupId,
                    artifactId = artifactIdFromPath,
                    version = versionFromPath,
                    classifier = classifier,
                    extension = extension
                )
                val fileContent = call.receiveStream().readBytes()
                val command = PublishArtifactCommand(
                    coordinates = coordinates,
                    fileContent = fileContent,
                    createdBy = createdBy
                )
                val result = artifactService.publish(command)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.Created)
                } else {
                    val cause = result.exceptionOrNull()
                    call.application.environment.log.error("Failed to publish Maven artifact: ${cause?.message}", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Failed to publish artifact: ${cause?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Error processing Maven artifact PUT request: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, "Failed to process artifact upload: ${e.message}")
            }
        }
    }

    get("/maven/{repoId}/{groupId}/{artifactId}/maven-metadata.xml") {
        // ... (existing GET metadata implementation, unchanged) ...
        val repoId =
            call.parameters["repoId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Repository ID is missing")
        val groupId =
            call.parameters["groupId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "GroupId is missing")
        val artifactId =
            call.parameters["artifactId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ArtifactId is missing")

        val result = artifactService.getAllVersions(groupId, artifactId)

        if (result.isSuccess) {
            val allArtifacts = result.getOrNull() ?: emptyList()
            val repoArtifacts = allArtifacts.filter { it.coordinates.repoId == repoId }

            if (repoArtifacts.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No artifacts found for $groupId:$artifactId in repo $repoId")
                return@get
            }

            val versions = repoArtifacts.map { it.coordinates.version }.distinct().sorted()
            val latestVersion = versions.lastOrNull()
            val releaseArtifacts =
                repoArtifacts.filter { it.status == ArtifactStatus.RELEASE || !it.coordinates.version.endsWith("-SNAPSHOT") }
            val releaseVersions = releaseArtifacts.map { it.coordinates.version }.distinct().sorted()
            val latestReleaseVersion = releaseVersions.lastOrNull()
            val lastUpdatedTimestamp = repoArtifacts.maxOfOrNull { it.updatedAt ?: it.createdAt } ?: Instant.now()
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC)
            val lastUpdatedFormatted = formatter.format(lastUpdatedTimestamp)
            val versionsXml = versions.joinToString("") { "    <version>$it</version>\n" }
            val metadataXml = """
            <metadata>
              <groupId>$groupId</groupId>
              <artifactId>$artifactId</artifactId>
              <versioning>
                ${latestVersion?.let { "<latest>$it</latest>" } ?: ""}
                ${latestReleaseVersion?.let { "<release>$it</release>" } ?: ""}
                <versions>
$versionsXml
                </versions>
                <lastUpdated>$lastUpdatedFormatted</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()
            call.respondText(metadataXml, ContentType.Application.Xml)
        } else {
            val cause = result.exceptionOrNull()
            call.application.environment.log.error(
                "Failed to retrieve versions for $groupId:$artifactId: ${cause?.message}",
                cause
            )
            call.respond(
                HttpStatusCode.InternalServerError,
                "Failed to retrieve artifact versions: ${cause?.message ?: "Unknown error"}"
            )
        }
    }

    get("/maven/{repoId}/{groupId}/{artifactId}/{version}/{file}") {
        val repoIdStr =
            call.parameters["repoId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Repository ID is missing")
        val groupId =
            call.parameters["groupId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "GroupId is missing")
        val artifactIdFromPath =
            call.parameters["artifactId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ArtifactId is missing")
        val versionFromPath =
            call.parameters["version"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Version is missing")
        val fileName =
            call.parameters["file"] ?: return@get call.respond(HttpStatusCode.BadRequest, "File name is missing")

        try {
            val extension = fileName.substringAfterLast('.', "")
            if (extension.isEmpty() || fileName.endsWith(".")) {
                return@get call.respond(HttpStatusCode.BadRequest, "File name missing or invalid extension: $fileName")
            }
            val nameWithoutExtension = fileName.substringBeforeLast('.')
            val expectedFilePrefix = "$artifactIdFromPath-$versionFromPath"
            var classifier: String? = null

            when {
                nameWithoutExtension == expectedFilePrefix -> { /* No classifier */
                }

                nameWithoutExtension.startsWith("$expectedFilePrefix-") -> {
                    classifier = nameWithoutExtension.substringAfter("$expectedFilePrefix-")
                    if (classifier.isEmpty()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Invalid empty classifier in file name: $fileName"
                        )
                    }
                }

                else -> {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "File name '$fileName' does not match expected format 'artifactId-version[-classifier].extension' " +
                                "based on path parameters artifactId '$artifactIdFromPath' and version '$versionFromPath'."
                    )
                }
            }

            val coordinates = ArtifactCoordinates(
                repoId = repoIdStr,
                groupId = groupId,
                artifactId = artifactIdFromPath,
                version = versionFromPath,
                classifier = classifier,
                extension = extension
            )

            val artifactResult = artifactService.getArtifact(coordinates)

            if (artifactResult.isSuccess) {
                val artifact = artifactResult.getOrNull()
                if (artifact != null && artifact.coordinates.repoId == repoIdStr) {
                    // Artifact found and belongs to the requested repository
                    val registryId =
                        RegistryId(UUID.fromString(repoIdStr)) // Convert String repoId to RegistryId (UUID)
                    val contentHash = artifact.contentHash // Assuming ContentHash is directly available

                    val contentResult = artifactService.retrieveArtifactContent(registryId, contentHash)
                    if (contentResult.isSuccess) {
                        val contentBytes = contentResult.getOrNull()
                        if (contentBytes != null) {
                            call.respondBytes(contentBytes, ContentType.defaultForFileExtension(extension))
                            // Optionally, log download event here if needed:
                            // artifactService.downloadArtifact(artifact.id, ...)
                        } else {
                            call.application.environment.log.error("Retrieved null content for $coordinates in repo $repoIdStr")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to retrieve artifact content (null)."
                            )
                        }
                    } else {
                        val cause = contentResult.exceptionOrNull()
                        call.application.environment.log.error(
                            "Failed to retrieve content for $coordinates in repo $repoIdStr: ${cause?.message}",
                            cause
                        )
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to retrieve artifact content: ${cause?.message ?: "Unknown error"}"
                        )
                    }
                } else {
                    // Artifact not found in this repo or with these exact coordinates
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Artifact $fileName not found in repository $repoIdStr with specified coordinates."
                    )
                }
            } else {
                val cause = artifactResult.exceptionOrNull()
                call.application.environment.log.error(
                    "Failed to get artifact metadata for $coordinates: ${cause?.message}",
                    cause
                )
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Failed to retrieve artifact metadata: ${cause?.message ?: "Unknown error"}"
                )
            }

        } catch (e: IllegalArgumentException) {
            // Catch UUID parsing errors for repoId or other parsing issues
            call.application.environment.log.warn("Bad request for artifact download: ${e.message}", e)
            call.respond(HttpStatusCode.BadRequest, "Invalid request parameters: ${e.message}")
        } catch (e: Exception) {
            call.application.environment.log.error(
                "Error processing Maven artifact GET request for $fileName: ${e.message}",
                e
            )
            call.respond(HttpStatusCode.InternalServerError, "Failed to process artifact download: ${e.message}")
        }
    }
}
