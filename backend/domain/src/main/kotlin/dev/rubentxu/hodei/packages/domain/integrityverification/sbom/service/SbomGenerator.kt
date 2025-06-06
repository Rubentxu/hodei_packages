package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.service


import dev.rubentxu.hodei.packages.domain.artifactmanagement.common.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationCompletedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationFailedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events.SbomGenerationStartedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomComponent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomGeneratorPort
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.ports.SbomRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.*

/**
 * Main SBOM generator class that delegates to a specific SbomGeneratorPort implementation
 * chosen by the SbomGeneratorFactory.
 * @param eventPublisher For publishing domain events.
 * @param sbomRepository For persisting generated SBOM documents.
 * @param sbomToolPath Optional path to an external SBOM generation tool. If null, internal generation is used.
 */
class SbomGenerator(
    private val eventPublisher: EventPublisher,
    private val sbomRepository: SbomRepository,
    sbomToolPath: String? = null
) : SbomGeneratorPort by SbomGeneratorFactory.create(eventPublisher, sbomRepository, sbomToolPath)

/**
 * Implementation of SbomGeneratorPort that uses internal analysis to generate SBOMs.
 * This implementation is basic and primarily designed for simple JSON structures like package.json.
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
        // artifactId is generated internally for each generation process
        val artifactId = UUID.randomUUID().toString()

        eventPublisher.publish(
            SbomGenerationStartedEvent(
                artifactId = artifactId,
                format = format
            )
        )

        return try {
            val components = extractComponents(artifactBytes)

            // Create a basic SbomDocument. More fields might be populated depending on SbomDocument's constructor defaults.
            val sbomDocument = SbomDocument(
                artifactId = artifactId,
                format = format,
                specVersion = determineSpecVersion(format), // Determine spec version based on format
                components = components
                // Other fields like creationTime, serialNumber, etc., will use defaults or be null
                // if not explicitly set and SbomDocument allows.
            )

            val result = sbomRepository.save(sbomDocument)

            if (result.isSuccess) {
                val endTime = Instant.now()
                val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()

                eventPublisher.publish(
                    SbomGenerationCompletedEvent(
                        sbomDocument = result.getOrNull()!!, // Should not be null if isSuccess
                        artifactId = artifactId,
                        durationMs = durationMs
                    )
                )
            } else {
                // If save fails, SbomGenerationFailedEvent should be published
                eventPublisher.publish(
                    SbomGenerationFailedEvent(
                        artifactId = artifactId,
                        format = format,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save SBOM document"
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

    private fun determineSpecVersion(format: SbomFormat): String {
        return when (format) {
            SbomFormat.CYCLONE_DX -> "1.4" // Common CycloneDX version
            SbomFormat.SPDX -> "2.3"       // Common SPDX version
            // Add other formats if necessary
            else -> "unknown"
        }
    }

    /**
     * Extracts components from artifact content (assumed to be JSON).
     * This is a basic implementation.
     */
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

            if (components.isEmpty()) { // Ensure at least one component if parsing yields nothing
                components.add(
                    SbomComponent(
                        name = "unknown-main-package",
                        version = "0.0.0",
                        type = "library"
                    )
                )
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            // Catch specific serialization exceptions from kotlinx.serialization
            // and rethrow as a more generic exception for the domain layer.
            throw IllegalArgumentException("Failed to parse artifact content as JSON: ${e.message}", e)
        } catch (e: Exception) {
            // If any other error occurs during parsing, add a default unknown component.
            // This ensures that an SBOM is still generated, albeit with minimal information.
            // This path might be hit if the JSON is valid but structure is unexpected by extractMainPackage/extractDependencies
            if (components.isEmpty()) {
                components.add(
                    SbomComponent(
                        name = "unknown-package-parsing-failed",
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
                    name = json["name"]?.jsonPrimitive?.content ?: "unknown-name",
                    version = json["version"]?.jsonPrimitive?.content ?: "unknown-version",
                    type = "library" // Default type
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null // Ignore errors and return null if main package info can't be extracted
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
                            type = "library" // Default type
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore errors during dependency extraction
        }
        return dependencies
    }
}

/**
 * Implementation of SbomGeneratorPort that uses an external command-line tool to generate SBOMs.
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
        // artifactId is generated internally for each generation process
        val artifactId = UUID.randomUUID().toString()

        eventPublisher.publish(
            SbomGenerationStartedEvent(
                artifactId = artifactId,
                format = format
            )
        )

        var tempFile: File? = null
        return try {
            tempFile = createTempFile(artifactId)
            tempFile.writeBytes(artifactBytes)

            val sbomContentString = executeSbomTool(tempFile, format)
            val sbomDocument = parseSbomDocument(sbomContentString, artifactId, format)

            val result = sbomRepository.save(sbomDocument)

            if (result.isSuccess) {
                val endTime = Instant.now()
                val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
                eventPublisher.publish(
                    SbomGenerationCompletedEvent(
                        sbomDocument = result.getOrNull()!!, // Should not be null if isSuccess
                        artifactId = artifactId,
                        durationMs = durationMs
                    )
                )
            } else {
                eventPublisher.publish(
                    SbomGenerationFailedEvent(
                        artifactId = artifactId,
                        format = format,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save SBOM document from external tool"
                    )
                )
            }
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
        } finally {
            tempFile?.delete()
        }
    }

    private fun createTempFile(artifactId: String): File {
        // Create in a temporary directory to ensure it's cleaned up
        return File.createTempFile("artifact-$artifactId-", ".tmp").apply {
            deleteOnExit() // Fallback cleanup
        }
    }

    private fun executeSbomTool(artifactFile: File, format: SbomFormat): String {
        val command = buildSbomToolCommand(artifactFile, format)
        try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true) // Merge stdout and stderr
                .start()

            // It's important to consume the output stream to prevent the process from blocking
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw IOException("SBOM tool execution failed with code $exitCode. Output: $output. Command: ${command.joinToString(" ")}")
            }
            return output
        } catch (e: IOException) {
            throw IOException("Error executing SBOM tool (${command.joinToString(" ")}): ${e.message}", e)
        }
    }

    private fun buildSbomToolCommand(artifactFile: File, format: SbomFormat): Array<String> {
        // These are example commands. Actual commands depend heavily on the specific SBOM tool.
        // Assuming the tool outputs to STDOUT if no output file is specified.
        return when (format) {
            SbomFormat.CYCLONE_DX -> arrayOf(
                sbomToolPath,
                "convert", // Common command for tools like syft, or 'generate', 'create'
                artifactFile.absolutePath,
                "--format", // Or similar flag for output format
                "cyclonedx-json" // Common output format string for CycloneDX JSON
                // Example: syft <file> -o cyclonedx-json
            )
            SbomFormat.SPDX -> arrayOf(
                sbomToolPath,
                "convert",
                artifactFile.absolutePath,
                "--format",
                "spdx-json" // Common output format string for SPDX JSON
                // Example: syft <file> -o spdx-json
            )
            // Add other formats if necessary
            else -> throw IllegalArgumentException("Unsupported SBOM format for external tool: $format")
        }
    }

    /**
     * Parses the SBOM content string generated by the external tool.
     * This is a STUB implementation. A real implementation would parse JSON/XML
     * based on the SbomFormat and populate the SbomDocument accordingly.
     */
    private fun parseSbomDocument(sbomContent: String, artifactId: String, format: SbomFormat): SbomDocument {
        // Basic SbomDocument. In a real scenario, 'sbomContent' would be parsed
        // to extract components, metadata, etc.
        // For now, we just create a document with minimal info.
        // If SbomDocument had a field for raw content, we could store sbomContent there.
        return SbomDocument(
            artifactId = artifactId,
            format = format,
            specVersion = determineSpecVersion(format),
            components = emptyList() // Placeholder; actual components would come from parsing sbomContent
        )
    }

     private fun determineSpecVersion(format: SbomFormat): String {
        return when (format) {
            SbomFormat.CYCLONE_DX -> "1.4"
            SbomFormat.SPDX -> "2.3"
            else -> "unknown"
        }
    }
}

/**
 * Factory for creating SbomGeneratorPort instances.
 * It decides whether to use an internal or external generator based on the presence of an SBOM tool path.
 */
class SbomGeneratorFactory {
    companion object {
        /**
         * Creates an SbomGeneratorPort.
         * @param eventPublisher For publishing domain events.
         * @param sbomRepository For persisting generated SBOM documents.
         * @param sbomToolPath Optional path to an external SBOM generation tool.
         *                     If provided, ExternalSbomGenerator is used; otherwise, InternalSbomGenerator.
         * @return An instance of SbomGeneratorPort.
         */
        fun create(
            eventPublisher: EventPublisher,
            sbomRepository: SbomRepository,
            sbomToolPath: String? = null
        ): SbomGeneratorPort {
            return if (!sbomToolPath.isNullOrBlank()) {
                ExternalSbomGenerator(eventPublisher, sbomRepository, sbomToolPath)
            } else {
                InternalSbomGenerator(eventPublisher, sbomRepository)
            }
        }
    }
}