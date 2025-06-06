package dev.rubentxu.hodei.packages.domain.ports.merkle

import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature


/**
 * Port for cryptographic services.
 * Defines the signing and verification operations necessary for the integrity
 * and authenticity of artifacts in the system.
 *
 * Following the principles of hexagonal architecture, this interface acts as a port
 * in the domain model that will be implemented by adapters in the infrastructure layer.
 */
interface CryptographicService {
    /**
     * Generates a signature for a content hash using a private key.
     *
     * @param contentHash The hash of the content to be signed
     * @param keyId Key identifier (typically the signer's email or name)
     * @param privateKeyData The bytes of the private key or null to use the default key
     * @param algorithm The signing algorithm to use (default is "Ed25519")
     * @return Result encapsulating the generated signature or an error
     */
    suspend fun sign(
        contentHash: ContentHash,
        keyId: String,
        privateKeyData: ByteArray? = null,
        algorithm: String = "Ed25519"
    ): Result<Signature>

    /**
     * Verifies the validity of a signature against a content hash.
     *
     * @param signature The signature to be verified
     * @param contentHash The hash of the content that should have been signed
     * @param publicKeyData The bytes of the public key or null to search by keyId
     * @return Result encapsulating true if the signature is valid, false if not, or an error
     */
    suspend fun verify(
        signature: Signature,
        contentHash: ContentHash,
        publicKeyData: ByteArray? = null
    ): Result<Boolean>

    /**
     * Generates a new key pair (public/private) for signing.
     *
     * @param keyId Identifier for the new key pair
     * @param algorithm The algorithm to use (default is "Ed25519")
     * @return Result encapsulating key-value pairs with the generated keys or an error
     */
    suspend fun generateKeyPair(
        keyId: String,
        algorithm: String = "Ed25519"
    ): Result<Map<String, ByteArray>>

    /**
     * Retrieves the public key associated with a keyId.
     *
     * @param keyId Key identifier
     * @return Result encapsulating the bytes of the public key or an error
     */
    suspend fun getPublicKey(keyId: String): Result<ByteArray?>
}