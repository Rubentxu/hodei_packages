package dev.rubentxu.hodei.packages.domain.model.merkle

import java.security.MessageDigest

/**
 * Representa un hash criptográfico del contenido de un artefacto o nodo en el grafo Merkle.
 * Este valor objeto encapsula tanto el algoritmo utilizado como el valor hash resultante.
 *
 * @param algorithm El algoritmo de hash utilizado (ej. "SHA-256", "SHA-512")
 * @param value El valor del hash en formato hexadecimal
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
         * Crea un hash a partir de un array de bytes utilizando el algoritmo especificado.
         *
         * @param content Los bytes de contenido a hashear
         * @param algorithm El algoritmo a utilizar (por defecto SHA-256)
         * @return Un objeto ContentHash con el resultado
         */
        fun create(content: ByteArray, algorithm: String = "SHA-256"): ContentHash {
            val digest = MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(content)
            val hashValue = hashBytes.joinToString("") { "%02x".format(it) }
            
            return ContentHash(algorithm, hashValue)
        }

        /**
         * Crea un hash a partir de una cadena de texto utilizando el algoritmo especificado.
         *
         * @param content El texto a hashear
         * @param algorithm El algoritmo a utilizar (por defecto SHA-256)
         * @return Un objeto ContentHash con el resultado
         */
        fun create(content: String, algorithm: String = "SHA-256"): ContentHash {
            return create(content.toByteArray(), algorithm)
        }
    }
}
