package dev.rubentxu.hodei.packages.domain.artifactmanagement.handler

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * Implementación de prueba de [FormatHandler] para verificar la funcionalidad por defecto de la interfaz.
 */
private class TestFormatHandler : FormatHandler {
    override fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?
    ): Result<Pair<ArtifactCoordinates, MetadataSource>> {
        // No se necesita para esta prueba específica, ya que probamos un método por defecto.
        throw NotImplementedError("extractCoordinates no es relevante para esta prueba de método por defecto.")
    }

    override fun extractMetadataWithSources(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?,
        artifactId: ArtifactId,
        userId: UserId
    ): Result<ArtifactMetadataWithSources> {
        // No se necesita para esta prueba específica.
        throw NotImplementedError("extractMetadataWithSources no es relevante para esta prueba de método por defecto.")
    }

    override fun determinePackagingType(
        filename: String,
        content: ByteArray
    ): Result<Pair<String, MetadataSource>> {
        // No se necesita para esta prueba específica.
        throw NotImplementedError("determinePackagingType no es relevante para esta prueba de método por defecto.")
    }

    override fun generateDescriptor(artifact: Artifact): Result<String> {
        // No se necesita para esta prueba específica.
        throw NotImplementedError("generateDescriptor no es relevante para esta prueba de método por defecto.")
    }

    override fun extractDependencies(content: ByteArray): Result<List<ArtifactDependency>> {
        // No se necesita para esta prueba específica.
        throw NotImplementedError("extractDependencies no es relevante para esta prueba de método por defecto.")
    }

    // El método validateMetadataConsistency tiene una implementación por defecto en la interfaz FormatHandler.
    // No es necesario sobreescribirlo aquí si solo queremos probar la implementación por defecto.
}

class FormatHandlerTest : StringSpec({

    "default validateMetadataConsistency should return success true" {
        val handler = TestFormatHandler()

        val testArtifactId = ArtifactId("art-id-123")
        val testUserId = UserId("user-id-456")

        val extractedMetadata = ArtifactMetadataWithSources(
            id = testArtifactId,
            createdBy = MetadataWithSource(testUserId, MetadataSource.SYSTEM_GENERATED),
            createdAt = MetadataWithSource(Instant.now(), MetadataSource.SYSTEM_GENERATED),
            description = MetadataWithSource("Extracted description", MetadataSource.CONTENT_EXTRACTED),
            licenses = null, // Puede ser null
            homepageUrl = null, // Puede ser null
            repositoryUrl = null, // Puede ser null
            sizeInBytes = MetadataWithSource(1024L, MetadataSource.SYSTEM_GENERATED),
            checksums = MetadataWithSource(mapOf("SHA-256" to "hash"), MetadataSource.SYSTEM_GENERATED),
            additionalMetadata = emptyMap()
        )
        val providedMetadata = mapOf(
            "description" to "Provided description",
            "customField" to "customValue"
        )

        // Llamar al método por defecto de la interfaz
        val result = handler.validateMetadataConsistency(extractedMetadata, providedMetadata)

        result.shouldBeSuccess { successValue ->
            successValue shouldBe true
        }
    }
})
