package dev.rubentxu.hodei.packages.domain.repository.merkle

import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash
import dev.rubentxu.hodei.packages.domain.model.merkle.Signature

/**
 * Puerto para servicios criptográficos.
 * Define las operaciones de firma y verificación necesarias para la integridad
 * y autenticidad de los artefactos en el sistema.
 * 
 * Siguiendo los principios de la arquitectura hexagonal, esta interfaz actúa como un puerto
 * en el modelo de dominio que será implementado por adaptadores en la capa de infraestructura.
 */
interface CryptographicService {
    /**
     * Genera una firma para un hash de contenido usando una clave privada.
     * 
     * @param contentHash El hash del contenido a firmar
     * @param keyId Identificador de la clave (generalmente el email o nombre del firmante)
     * @param privateKeyData Los bytes de la clave privada o null para usar la clave por defecto
     * @param algorithm El algoritmo de firma a utilizar (por defecto "Ed25519")
     * @return Resultado encapsulando la firma generada o un error
     */
    suspend fun sign(
        contentHash: ContentHash,
        keyId: String,
        privateKeyData: ByteArray? = null,
        algorithm: String = "Ed25519"
    ): Result<Signature>
    
    /**
     * Verifica la validez de una firma contra un hash de contenido.
     * 
     * @param signature La firma a verificar
     * @param contentHash El hash del contenido que debería haber sido firmado
     * @param publicKeyData Los bytes de la clave pública o null para buscar por keyId
     * @return Resultado encapsulando true si la firma es válida, false si no, o un error
     */
    suspend fun verify(
        signature: Signature,
        contentHash: ContentHash,
        publicKeyData: ByteArray? = null
    ): Result<Boolean>
    
    /**
     * Genera un nuevo par de claves (pública/privada) para firma.
     * 
     * @param keyId Identificador para el nuevo par de claves
     * @param algorithm El algoritmo a utilizar (por defecto "Ed25519")
     * @return Resultado encapsulando pares clave-valor con las claves generadas o un error
     */
    suspend fun generateKeyPair(
        keyId: String,
        algorithm: String = "Ed25519"
    ): Result<Map<String, ByteArray>>
    
    /**
     * Recupera la clave pública asociada a un keyId.
     * 
     * @param keyId Identificador de la clave
     * @return Resultado encapsulando los bytes de la clave pública o un error
     */
    suspend fun getPublicKey(keyId: String): Result<ByteArray?>
}
