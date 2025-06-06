package dev.rubentxu.hodei.packages.domain.service.sbom

import dev.rubentxu.hodei.packages.domain.common.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationCompletedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationFailedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationStartedEvent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomGeneratorPort
import dev.rubentxu.hodei.packages.domain.ports.sbom.SbomRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.*


class SbomGenerator(
    private val eventPublisher: EventPublisher,
    private val sbomRepository: SbomRepository,
    sbomToolPath: String? = null
) : SbomGeneratorPort by SbomGeneratorFactory.create(eventPublisher, sbomRepository, sbomToolPath)

/**
 * Implementación del generador SBOM que utiliza análisis interno.
 */
class InternalSbomGenerator(
    private val eventPublisher: EventPublisher,
    private val sbomRepository: SbomRepository
) : SbomGeneratorPort {

    override suspend fun generate(
        artifactBytes: ByteArray,
        format: SbomFormat
    ): Result<SbomDocument> {
        val startTime = Instant.now()
        val artifactId = UUID.randomUUID().toString()

        eventPublisher.publish(
            SbomGenerationStartedEvent(
                artifactId = artifactId,
                format = format
            )
        )

        return try {
            val components = extractComponents(artifactBytes)

            val sbomDocument = SbomDocument(
                artifactId = artifactId,
                format = format,
                specVersion = "1.4",
                components = components
            )

            val result = sbomRepository.save(sbomDocument)

            if (result.isSuccess) {
                val endTime = Instant.now()
                val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()

                eventPublisher.publish(
                    SbomGenerationCompletedEvent(
                        sbomDocument = result.getOrNull()!!,
                        artifactId = artifactId,
                        durationMs = durationMs
                    )
                )
            }

            result
        } catch (e: Exception) {
            eventPublisher.publish(
                SbomGenerationFailedEvent(
                    artifactId = artifactId,
                    format = format,
                    errorMessage = e.message ?: "Unknown error during SBOM generation"
                )
            )

            Result.failure(e)
        }
    }

    private fun extractComponents(artifactContent: ByteArray): List<SbomComponent> {
        val components = mutableListOf<SbomComponent>()

        try {
            val contentString = artifactContent.toString(Charsets.UTF_8)
            val json = Json.parseToJsonElement(contentString).jsonObject

            val mainPackage = extractMainPackage(json)
            if (mainPackage != null) {
                components.add(mainPackage)
            }

            components.addAll(extractDependencies(json))

        } catch (e: Exception) {
            if (components.isEmpty()) {
                components.add(
                    SbomComponent(
                        name = "unknown-package",
                        version = "0.0.0",
                        type = "library"
                    )
                )
            }
        }

        return components
    }

    private fun extractMainPackage(json: JsonObject): SbomComponent? {
        return try {
            if (json.containsKey("name") && json.containsKey("version")) {
                SbomComponent(
                    name = json["name"]?.jsonPrimitive?.content ?: "unknown",
                    version = json["version"]?.jsonPrimitive?.content ?: "0.0.0",
                    type = "library"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDependencies(json: JsonObject): List<SbomComponent> {
        val dependencies = mutableListOf<SbomComponent>()

        try {
            if (json.containsKey("dependencies")) {
                val deps = json["dependencies"]?.jsonObject ?: return dependencies

                for (dep in deps) {
                    dependencies.add(
                        SbomComponent(
                            name = dep.key,
                            version = dep.value.jsonPrimitive.content,
                            type = "library"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignorar errores en la extracción de dependencias
        }

        return dependencies
    }
}

/**
 * Implementación del generador SBOM que utiliza una herramienta externa.
 */
class ExternalSbomGenerator(
    private val eventPublisher: EventPublisher,
    private val sbomRepository: SbomRepository,
    private val sbomToolPath: String
) : SbomGeneratorPort {

    override suspend fun generate(
        artifactBytes: ByteArray,
        format: SbomFormat
    ): Result<SbomDocument> {
        val startTime = Instant.now()
        val artifactId = UUID.randomUUID().toString()

        eventPublisher.publish(
            SbomGenerationStartedEvent(
                artifactId = artifactId,
                format = format
            )
        )

        return try {
            val tempFile = createTempFile(artifactId)
            tempFile.writeBytes(artifactBytes)

            val sbomContent = executeSbomTool(tempFile, format)

            val sbomDocument = parseSbomDocument(sbomContent, artifactId, format)

            val result = sbomRepository.save(sbomDocument)

            if (result.isSuccess) {
                val endTime = Instant.now()
                val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()

                eventPublisher.publish(
                    SbomGenerationCompletedEvent(
                        sbomDocument = result.getOrNull()!!,
                        artifactId = artifactId,
                        durationMs = durationMs
                    )
                )
            }

            tempFile.delete()

            result
        } catch (e: Exception) {
            eventPublisher.publish(
                SbomGenerationFailedEvent(
                    artifactId = artifactId,
                    format = format,
                    errorMessage = e.message ?: "Error during SBOM generation with external tool"
                )
            )

            Result.failure(e)
        }
    }

    private fun createTempFile(artifactId: String): File {
        return File.createTempFile("artifact-$artifactId", ".tmp")
    }

    private fun executeSbomTool(artifactFile: File, format: SbomFormat): String {
        val command = buildSbomToolCommand(artifactFile, format)

        try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw IOException("SBOM tool execution failed with code $exitCode: $output")
            }

            return output
        } catch (e: IOException) {
            throw IOException("Error executing SBOM tool: ${e.message}", e)
        }
    }

    private fun buildSbomToolCommand(artifactFile: File, format: SbomFormat): Array<String> {
        return when (format) {
            SbomFormat.CYCLONE_DX -> arrayOf(
                sbomToolPath,
                "analyze",
                "-i",
                artifactFile.absolutePath,
                "-o",
                "sbom.json",
                "-f",
                "json"
            )

            SbomFormat.SPDX -> arrayOf(sbomToolPath, "-i", artifactFile.absolutePath, "-o", "sbom.spdx")
            else -> throw IllegalArgumentException("Unsupported SBOM format: $format")
        }
    }

    private fun parseSbomDocument(sbomContent: String, artifactId: String, format: SbomFormat): SbomDocument {
        return SbomDocument(
            artifactId = artifactId,
            format = format,
            specVersion = "1.4",
            components = listOf()
        )
    }
}

/**
 * Fabrica para crear instancias de `SbomGeneratorService`.
 */
class SbomGeneratorFactory {
    companion object {
        fun create(
            eventPublisher: EventPublisher,
            sbomRepository: SbomRepository,
            sbomToolPath: String? = null
        ): SbomGeneratorPort {
            return if (sbomToolPath != null) {
                ExternalSbomGenerator(eventPublisher, sbomRepository, sbomToolPath)
            } else {
                InternalSbomGenerator(eventPublisher, sbomRepository)
            }
        }
    }
}