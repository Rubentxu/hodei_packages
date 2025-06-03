package dev.rubentxu.hodei.packages.domain.model.merkle

import java.time.Instant
import java.util.UUID

/**
 * Representa una firma criptográfica de un hash de contenido.
 * Las firmas son utilizadas para verificar la autenticidad e integridad de los nodos
 * en el grafo Merkle.
 *
 * @param value El valor de la firma criptográfica (codificado en base64 generalmente)
 * @param algorithm El algoritmo de firma utilizado (ej. "Ed25519", "RSA-PSS", etc.)
 * @param contentHash El hash del contenido que ha sido firmado
 * @param keyId Identificador de la clave utilizada para la firma (generalmente email o nombre del firmante)
 * @param creationTime Momento de creación de la firma
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
        
        id = generateSignatureId(value, creationTime)
    }

    /**
     * Genera un identificador único para la firma basado en su valor y momento de creación.
     */
    private fun generateSignatureId(value: String, creationTime: Instant): String {
        return UUID.nameUUIDFromBytes("$value:${creationTime.toEpochMilli()}".toByteArray()).toString()
    }
}
