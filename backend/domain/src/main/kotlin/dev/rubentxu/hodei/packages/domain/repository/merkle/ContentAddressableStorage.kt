package dev.rubentxu.hodei.packages.domain.repository.merkle

import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash

/**
 * Puerto para el almacenamiento direccionable por contenido.
 * Define las operaciones para almacenar y recuperar contenido utilizando su hash como identificador.
 * Este es un componente clave para la implementación de grafos Merkle y verificación de integridad.
 * 
 * Siguiendo los principios de la arquitectura hexagonal, esta interfaz actúa como un puerto
 * en el modelo de dominio que será implementado por adaptadores en la capa de infraestructura.
 */
interface ContentAddressableStorage {
    /**
     * Almacena contenido en el sistema y devuelve su hash criptográfico.
     * 
     * @param content Los bytes del contenido a almacenar
     * @param algorithm El algoritmo de hash a utilizar (por defecto SHA-256)
     * @return Resultado encapsulando el hash del contenido o un error
     */
    suspend fun store(content: ByteArray, algorithm: String = "SHA-256"): Result<ContentHash>
    
    /**
     * Recupera contenido mediante su hash.
     * 
     * @param contentHash El hash del contenido a recuperar
     * @return Resultado encapsulando los bytes del contenido o un error
     */
    suspend fun retrieve(contentHash: ContentHash): Result<ByteArray?>
    
    /**
     * Verifica si un contenido con el hash especificado existe en el almacenamiento.
     * 
     * @param contentHash El hash del contenido a verificar
     * @return Resultado encapsulando true si existe, false si no, o un error
     */
    suspend fun exists(contentHash: ContentHash): Result<Boolean>
    
    /**
     * Elimina contenido del almacenamiento por su hash.
     * 
     * @param contentHash El hash del contenido a eliminar
     * @return Resultado encapsulando true si se eliminó, false si no existía, o un error
     */
    suspend fun delete(contentHash: ContentHash): Result<Boolean>
}
