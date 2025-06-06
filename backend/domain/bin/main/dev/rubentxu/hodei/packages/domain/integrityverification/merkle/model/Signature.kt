package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model

import java.time.Instant
import java.util.UUID

/**
 * Representa una firma digital de un hash de contenido.
 * Se utiliza para verificar la autenticidad de los grafos Merkle.
 *
 * @param value Valor de la firma en formato base64
 * @param algorithm Algoritmo de firma utilizado (e.g., "Ed25519", "RSA")
 * @param contentHash Hash del contenido que se firma
 * @param keyId Identificador de la clave utilizada para firmar
 */
data class Signature(
    val value: String,
    val algorithm: String,
    val contentHash: ContentHash,
    val keyId: String
) {
    val id: String

    init {
        require(value.isNotBlank()) { "Signature value cannot be blank" }
        require(algorithm.isNotBlank()) { "Signature algorithm cannot be blank" }
        require(keyId.isNotBlank()) { "Key ID cannot be blank" }
        
        id = generateSignatureId(value, Instant.now())
    }

    /**
     * Genera un identificador único para la firma basado en su valor y momento de creación.
     */
    private fun generateSignatureId(value: String, creationTime: Instant): String {
        return UUID.nameUUIDFromBytes("$value:${creationTime.toEpochMilli()}".toByteArray()).toString()
    }
}
