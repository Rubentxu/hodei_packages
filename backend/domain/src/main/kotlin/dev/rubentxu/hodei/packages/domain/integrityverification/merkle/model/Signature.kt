package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model

import java.time.Instant
import java.util.*

/**
 * Represents a digital signature for a Merkle graph or a specific node within it.
 * This signature can be used to verify the integrity and authenticity of the data.
 *
 * @param value The actual signature value (e.g., a base64-encoded string).
 * @param algorithm The cryptographic algorithm used to generate the signature (e.g., "Ed25519", "RSA").
 * @param contentHash The hash of the content being signed.
 * @param keyId An identifier for the key used to create the signature (e.g., a key fingerprint, user ID, or certificate ID).
 * @param creationTime The timestamp when the signature was created.
 */
data class Signature(
    val value: String,
    val algorithm: String,
    val contentHash: ContentHash,
    val keyId: String,
    val creationTime: Instant = Instant.now()
) {
    val id: String

    init {
        require(value.isNotBlank()) { "Signature value cannot be blank" }
        require(algorithm.isNotBlank()) { "Signature algorithm cannot be blank" }
        require(keyId.isNotBlank()) { "Key ID cannot be blank" }

        id = generateSignatureId(value, algorithm, contentHash, keyId, creationTime)
    }

    /**
     * Generates a unique identifier for the signature based on its value, algorithm, contentHash, keyId, and creationTime.
     * This implementation ensures that the same input parameters will always generate the same ID.
     */
    private fun generateSignatureId(
        value: String,
        algorithm: String,
        contentHash: ContentHash,
        keyId: String,
        creationTime: Instant
    ): String {
        // Create a consistent string representation of all relevant fields
        // The specific order and format ensure deterministic ID generation
        val nameString = "algorithm:$algorithm;value:$value;keyId:$keyId;contentHashAlgorithm:${contentHash.algorithm};contentHashValue:${contentHash.value};creationTimeMillis:${creationTime.toEpochMilli()}"
        
        // Use the built-in nameUUIDFromBytes which implements RFC 4122 UUID type 3 (name-based)
        // This guarantees deterministic UUIDs from the same input
        return UUID.nameUUIDFromBytes(nameString.toByteArray(Charsets.UTF_8)).toString()
    }
}