package dev.rubentxu.hodei.packages.domain.artifactmanagement.command

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactMetadata
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.RegistryId


/**
 * Represents a command to upload a new artifact to the system.
 * This data class encapsulates all necessary information required by the `ArtifactService`
 * to perform an artifact upload operation. Using a command object promotes a cleaner API
 * for the service and better organization of parameters.
 *
 * @property registryId The unique identifier of the registry where the artifact will be published.
 * @property content The binary content of the artifact as a [ByteArray].
 * @property filename The original filename of the artifact as provided by the uploader.
 *                    This is often used by [FormatHandler] implementations to infer initial coordinates or type.
 * @property artifactType The [ArtifactType] of the artifact, indicating its format (e.g., MAVEN, NPM, DOCKER).
 *                        This helps in selecting the appropriate [FormatHandler].
 * @property createdBy The [UserId] of the user or system principal initiating the upload.
 *                     This is used for auditing and setting the `createdBy` field in [ArtifactMetadata].
 * @property providedMetadata An [ArtifactMetadata] object containing descriptive information about the artifact
 *                            as provided by the uploader. The service will use this as a base and will
 *                            override or set system-managed fields like `id`, `createdBy`, `createdAt`,
 *                            `updatedAt`, and `sizeInBytes` (from the actual content).
 */
data class UploadArtifactCommand(
    val registryId: RegistryId,
    val content: ByteArray,
    val filename: String,
    val artifactType: ArtifactType,
    val createdBy: UserId,
    val providedMetadata: ArtifactMetadata // El llamador provee este objeto estructurado
) {
    // Es buena práctica sobreescribir equals y hashCode para data classes con ByteArrays
    // para asegurar una comparación basada en el contenido del array y no en su referencia.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadArtifactCommand

        if (registryId != other.registryId) return false
        if (!content.contentEquals(other.content)) return false
        if (filename != other.filename) return false
        if (artifactType != other.artifactType) return false
        if (createdBy != other.createdBy) return false
        if (providedMetadata != other.providedMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = registryId.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + artifactType.hashCode()
        result = 31 * result + createdBy.hashCode()
        result = 31 * result + providedMetadata.hashCode()
        return result
    }
}