package dev.rubentxu.hodei.packages.domain.registrymanagement.service

import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.command.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.events.ArtifactRegistryEvent
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.ports.RegistryRepository
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

// Helper extension functions for Result for cleaner composition
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}

/**
 * Domain service that encapsulates business logic related to artifact registry management.
 * Implements ArtifactRegistryService.
 */
class RegistryService(
    private val registryRepository: RegistryRepository,
    private val eventPublisher: (ArtifactRegistryEvent) -> Unit
) : ArtifactRegistryService {
    // --- Create Operations ---
    override suspend fun createHostedRegistry(command: CreateHostedRegistryCommand): Result<HostedRegistry> =
        createRegistryInternal(
            name = command.name,
            registryFactory = { id ->
                HostedRegistry(
                    id = id,
                    name = command.name,
                    format = command.format,
                    description = command.description,
                    storageConfig = command.storageConfig,
                    deploymentPolicy = command.deploymentPolicy,
                    cleanupPolicy = command.cleanupPolicy,
                    specificFormatConfig = command.specificFormatConfig,
                    online = command.online
                )
            },
            requestedBy = command.requestedBy,
            errorContext = "hosted"
        )

    override suspend fun createProxyRegistry(command: CreateProxyRegistryCommand): Result<ProxyRegistry> =
        createRegistryInternal(
            name = command.name,
            registryFactory = { id ->
                ProxyRegistry(
                    id = id,
                    name = command.name,
                    format = command.format,
                    description = command.description,
                    storageConfig = command.storageConfig,
                    proxyConfig = command.proxyConfig,
                    cleanupPolicy = command.cleanupPolicy,
                    online = command.online
                )
            },
            requestedBy = command.requestedBy,
            errorContext = "proxy"
        )

    override suspend fun createGroupRegistry(command: CreateGroupRegistryCommand): Result<GroupRegistry> =
        createRegistryInternal(
            name = command.name,
            registryFactory = { id ->
                GroupRegistry(
                    id = id,
                    name = command.name,
                    format = command.format,
                    description = command.description,
                    storageConfig = command.storageConfig,
                    groupConfig = command.groupConfig,
                    cleanupPolicy = command.cleanupPolicy,
                    online = command.online
                )
            },
            requestedBy = command.requestedBy,
            errorContext = "group"
        )

    override suspend fun updateRegistry(command: UpdateRegistryCommand): Result<Registry> {
        try {
            return registryRepository.findById(command.registryId).flatMap { existingRegistry ->
                if (existingRegistry == null) {
                    Result.failure(IllegalArgumentException("ArtifactRegistry with ID '${command.registryId}' not found"))
                } else {
                    val changes = mutableMapOf<String, Any?>()
                    val updatedRegistry = buildUpdatedRegistry(existingRegistry, command, changes)

                    if (updatedRegistry == existingRegistry && changes.isEmpty()) {
                        Result.success(existingRegistry)
                    } else {
                        registryRepository.save(updatedRegistry).fold(
                            onSuccess = { savedRegistry ->
                                publishEvent(
                                    ArtifactRegistryEvent.ArtifactRegistryUpdated(
                                        registryId = savedRegistry.id.value,
                                        name = savedRegistry.name,
                                        updatedBy = command.requestedBy,
                                        timestamp = Instant.now(),
                                        changes = changes
                                    )
                                )
                                Result.success(savedRegistry)
                            },
                            onFailure = { exception -> Result.failure(exception) }
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(RuntimeException("Error updating registry '${command.registryId}': ${e.message}", e))
        }
    }

    override suspend fun deleteRegistry(command: DeleteRegistryCommand): Result<Boolean> {
        try {
            return registryRepository.findById(command.registryId).flatMap { registry ->
                if (registry == null) {
                    Result.success(false) // Not found, deletion is idempotent
                } else {
                    registryRepository.deleteById(command.registryId).flatMap { deleted ->
                        if (deleted) {
                            publishEvent(
                                ArtifactRegistryEvent.ArtifactRegistryDeleted(
                                    registryId = registry.id.value,
                                    name = registry.name,
                                    deletedBy = command.requestedBy,
                                    timestamp = Instant.now()
                                )
                            )
                        }
                        Result.success(deleted)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(RuntimeException("Error deleting registry '${command.registryId}': ${e.message}", e))
        }
    }

    override suspend fun findRegistryById(command: FindRegistryByIdCommand): Result<Registry?> =
        try {
            registryRepository.findById(command.registryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(RuntimeException("Error finding registry by ID '${command.registryId}': ${e.message}", e))
        }

    override suspend fun findRegistryByName(command: FindRegistryByNameCommand): Result<Registry?> =
        try {
            registryRepository.findByName(command.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(RuntimeException("Error finding registry by name '${command.name}': ${e.message}", e))
        }

    override suspend fun findRegistriesByFormat(command: FindRegistriesByFormatCommand): Result<List<Registry>> =
        try {
            registryRepository.findAll(command.format)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(RuntimeException("Error finding registries by format '${command.format}': ${e.message}", e))
        }

    private suspend inline fun <reified R : Registry> createRegistryInternal(
        name: String,
        crossinline registryFactory: (RegistryId) -> R, // Use crossinline for non-local return from lambda
        requestedBy: UserId,
        errorContext: String
    ): Result<R> {
        try {
            return registryRepository.existsByName(name).flatMap { exists ->
                if (exists) {
                    Result.failure(IllegalStateException("An artifact registry with name '$name' already exists"))
                } else {
                    val newRegistryId = RegistryId.random()
                    val newRegistry = registryFactory(newRegistryId)

                    registryRepository.save(newRegistry).flatMap { savedRegistry ->
                        val typedSavedRegistry = savedRegistry as? R
                            ?: return@flatMap Result.failure(
                                IllegalStateException(
                                    "Saved $errorContext registry was not of the expected type ${R::class.simpleName}. " +
                                            "Actual type: ${savedRegistry::class.simpleName}"
                                )
                            )

                        publishEvent(
                            ArtifactRegistryEvent.ArtifactRegistryCreated(
                                registryId = typedSavedRegistry.id.value,
                                name = typedSavedRegistry.name,
                                type = typedSavedRegistry.format,
                                createdBy = requestedBy,
                                timestamp = Instant.now() // Consider using a creation timestamp from the saved entity if available
                            )
                        )
                        Result.success(typedSavedRegistry)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation exceptions to allow coroutine machinery to handle them
        } catch (e: Exception) {
            // Catch-all for unexpected errors during the process, including those from existsByName or save if they don't return Result
            return Result.failure(RuntimeException("Error creating $errorContext artifact registry '$name': ${e.message}", e))
        }
    }

    private fun buildUpdatedRegistry(
        existing: Registry,
        command: UpdateRegistryCommand,
        changes: MutableMap<String, Any?>
    ): Registry {
        return when (existing) {
            is HostedRegistry -> existing.copy(
                description = updateAndTrackChange(command.description, existing.description, "description", changes),
                online = updateAndTrackChange(command.online, existing.online, "online", changes),
                storageConfig = updateAndTrackChange(command.storageConfig, existing.storageConfig, "storageConfig", changes),
                cleanupPolicy = updateAndTrackChange(command.cleanupPolicy, existing.cleanupPolicy, "cleanupPolicy", changes),
                deploymentPolicy = updateAndTrackChange(command.deploymentPolicy, existing.deploymentPolicy, "deploymentPolicy", changes),
                specificFormatConfig = updateAndTrackChange(command.specificFormatConfig, existing.specificFormatConfig, "specificFormatConfig", changes)
            )
            is ProxyRegistry -> existing.copy(
                description = updateAndTrackChange(command.description, existing.description, "description", changes),
                online = updateAndTrackChange(command.online, existing.online, "online", changes),
                storageConfig = updateAndTrackChange(command.storageConfig, existing.storageConfig, "storageConfig", changes),
                cleanupPolicy = updateAndTrackChange(command.cleanupPolicy, existing.cleanupPolicy, "cleanupPolicy", changes),
                proxyConfig = updateAndTrackChange(command.proxyConfig, existing.proxyConfig, "proxyConfig", changes)
            )
            is GroupRegistry -> existing.copy(
                description = updateAndTrackChange(command.description, existing.description, "description", changes),
                online = updateAndTrackChange(command.online, existing.online, "online", changes),
                storageConfig = updateAndTrackChange(command.storageConfig, existing.storageConfig, "storageConfig", changes),
                cleanupPolicy = updateAndTrackChange(command.cleanupPolicy, existing.cleanupPolicy, "cleanupPolicy", changes),
                groupConfig = updateAndTrackChange(command.groupConfig, existing.groupConfig, "groupConfig", changes)
            )
        }
    }

    private fun <T> updateAndTrackChange(newValue: T?, existingValue: T, fieldName: String, changes: MutableMap<String, Any?>): T {
        return newValue?.takeIf { it != existingValue }?.also {
            changes[fieldName] = it
        } ?: existingValue
    }

    /**
     * Helper method to publish events safely.
     * Logs errors but does not let event publishing failures fail the main operation.
     */
    private fun publishEvent(event: ArtifactRegistryEvent) {
        try {
            eventPublisher(event)
        } catch (e: Exception) {
            // Consider using a proper logger in a real application
            System.err.println("Error publishing event $event: ${e.message}")
            // Optionally, re-throw if event publishing is critical and should fail the operation
            // throw RuntimeException("Failed to publish event $event", e)
        }
    }
}