package dev.rubentxu.hodei.packages.domain.policymanagement.ports

import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.SecurityPolicy

/**
 * Port for managing security policies.
 * Defines the operations for persisting and retrieving security policies.
 */
interface PolicyRepository {
    /**
     * Saves a security policy.
     * @param policy The security policy to save
     * @return Result with the ID of the saved policy or an error
     */
    suspend fun save(policy: SecurityPolicy): Result<PolicyId>

    /**
     * Finds a security policy by its ID.
     * @param id The ID of the policy to search for
     * @return Result with the found security policy, or null if it does not exist, or an error
     */
    suspend fun findById(id: PolicyId): Result<SecurityPolicy?>

    /**
     * Finds all security policies.
     * @return Result with the list of all security policies or an error
     */
    suspend fun findAll(): Result<List<SecurityPolicy>>
}