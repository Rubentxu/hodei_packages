package dev.rubentxu.hodei.packages.domain.registrymanagement.service

import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import dev.rubentxu.hodei.packages.domain.registrymanagement.command.*
import dev.rubentxu.hodei.packages.domain.registrymanagement.model.*

/**
 * Interface for managing artifact registries.
 * Provides semantic, explicit method names for all registry operations.
 */
interface ArtifactRegistryService {
    suspend fun createHostedRegistry(command: CreateHostedRegistryCommand): Result<HostedRegistry>
    suspend fun createProxyRegistry(command: CreateProxyRegistryCommand): Result<ProxyRegistry>
    suspend fun createGroupRegistry(command: CreateGroupRegistryCommand): Result<GroupRegistry>
    suspend fun updateRegistry(command: UpdateRegistryCommand): Result<Registry>
    suspend fun deleteRegistry(command: DeleteRegistryCommand): Result<Boolean>
    suspend fun findRegistryById(command: FindRegistryByIdCommand): Result<Registry?>
    suspend fun findRegistryByName(command: FindRegistryByNameCommand): Result<Registry?>
    suspend fun findRegistriesByFormat(command: FindRegistriesByFormatCommand): Result<List<Registry>>
}
