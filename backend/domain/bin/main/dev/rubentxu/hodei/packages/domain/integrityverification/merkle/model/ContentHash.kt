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
         * Crea un ContentHash a partir de un contenido y un algoritmo.
         * En una implementación real, esto calcularía el hash del contenido.
         *
         * @param content Contenido a hashear
         * @param algorithm Algoritmo a utilizar (opcional, por defecto "SHA-256")
         * @return ContentHash con el hash calculado
         */
        fun create(content: String, algorithm: String = "SHA-256"): ContentHash {
            // En una implementación real, aquí se calcularía el hash
            return ContentHash(algorithm, content)
        }
    }
}
