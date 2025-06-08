package dev.rubentxu.hodei.packages.domain.registrymanagement.command

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactType
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*
import java.util.UUID

/**
 * Base interface for all registry-related commands
 */
sealed interface RegistryCommand {
    val commandId: UUID
    val requestedBy: UserId
}

/**
 * Base interface for commands that create registries
 */
sealed interface CreateRegistryCommand : RegistryCommand {
    val name: String
    val format: ArtifactType
    val description: String?
    val storageConfig: StorageConfig
    val cleanupPolicy: CleanupPolicy?
}

/**
 * Command to create a hosted artifact registry
 */
data class CreateHostedRegistryCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    override val name: String,
    override val format: ArtifactType,
    override val description: String?,
    override val storageConfig: StorageConfig,
    override val cleanupPolicy: CleanupPolicy? = null,
    val deploymentPolicy: DeploymentPolicy = DeploymentPolicy.ALLOW_REDEPLOY_SNAPSHOT,
    val specificFormatConfig: Map<String, String>? = null,
    val online: Boolean = true
) : CreateRegistryCommand

/**
 * Command to create a proxy artifact registry
 */
data class CreateProxyRegistryCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    override val name: String,
    override val format: ArtifactType,
    override val description: String?,
    override val storageConfig: StorageConfig,
    override val cleanupPolicy: CleanupPolicy? = null,
    val proxyConfig: ProxyConfig,
    val specificFormatConfig: Map<String, String>? = null,
    val online: Boolean = true
) : CreateRegistryCommand

/**
 * Command to create a group artifact registry
 */
data class CreateGroupRegistryCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    override val name: String,
    override val format: ArtifactType,
    override val description: String?,
    override val storageConfig: StorageConfig,
    override val cleanupPolicy: CleanupPolicy? = null,
    val groupConfig: GroupConfig,
    val online: Boolean = true
) : CreateRegistryCommand

/**
 * Command to update an existing artifact registry
 */
data class UpdateRegistryCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    val registryId: RegistryId,
    val description: String? = null,
    val online: Boolean? = null,
    val storageConfig: StorageConfig? = null,
    val cleanupPolicy: CleanupPolicy? = null,
    // Type-specific updates
    val deploymentPolicy: DeploymentPolicy? = null,
    val specificFormatConfig: Map<String, String>? = null,
    val proxyConfig: ProxyConfig? = null,
    val groupConfig: GroupConfig? = null
) : RegistryCommand

/**
 * Command to delete an artifact registry
 */
data class DeleteRegistryCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    val registryId: RegistryId
) : RegistryCommand

/**
 * Base interface for registry query commands
 */
sealed interface RegistryQueryCommand : RegistryCommand

/**
 * Command to find registries by format (type)
 */
data class FindRegistriesByFormatCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    val format: ArtifactType
) : RegistryQueryCommand

/**
 * Command to find a registry by ID
 */
data class FindRegistryByIdCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    val registryId: RegistryId
) : RegistryQueryCommand

/**
 * Command to find a registry by name
 */
data class FindRegistryByNameCommand(
    override val commandId: UUID = UUID.randomUUID(),
    override val requestedBy: UserId,
    val name: String
) : RegistryQueryCommand