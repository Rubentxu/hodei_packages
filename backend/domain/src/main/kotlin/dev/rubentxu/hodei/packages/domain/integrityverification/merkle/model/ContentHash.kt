package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model

import java.security.MessageDigest

/**
 * Representa un hash criptográfico de contenido.
 * Se utiliza para verificar la integridad de los archivos y directorios
 * en el grafo Merkle.
 *
 * @param algorithm Algoritmo de hash utilizado (e.g., "SHA-256", "SHA-512")
 * @param value Valor hexadecimal del hash
 */
data class ContentHash(
    val algorithm: String,
    val value: String
) {
    init {
        require(algorithm.isNotBlank()) { "Hash algorithm cannot be blank" }
        require(value.isNotBlank()) { "Hash value cannot be blank" }
    }

    companion object {
        /**
         * Crea un ContentHash a partir de un contenido String y un algoritmo.
         *
         * @param content Contenido en forma de String a hashear
         * @param algorithm Algoritmo a utilizar (opcional, por defecto "SHA-256")
         * @return ContentHash con el hash calculado
         */
        fun create(content: String, algorithm: String = "SHA-256"): ContentHash {
            val bytes = content.toByteArray(Charsets.UTF_8)
            return createFromBytes(bytes, algorithm)
        }

        /**
         * Crea un ContentHash a partir de un contenido en bytes y un algoritmo.
         * Este método es más adecuado para contenido binario o cuando ya se tiene
         * el contenido en forma de ByteArray.
         *
         * @param bytes Contenido en forma de ByteArray a hashear
         * @param algorithm Algoritmo a utilizar (opcional, por defecto "SHA-256")
         * @return ContentHash con el hash calculado
         */
        fun createFromBytes(bytes: ByteArray, algorithm: String = "SHA-256"): ContentHash {
            val digest = MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(bytes)
            val hashValue = hashBytes.joinToString("") { "%02x".format(it) }
            return ContentHash(algorithm, hashValue)
        }
    }
    
    /**
     * Convierte este hash a su representación como ByteArray
     */
    fun toByteArray(): ByteArray {
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
