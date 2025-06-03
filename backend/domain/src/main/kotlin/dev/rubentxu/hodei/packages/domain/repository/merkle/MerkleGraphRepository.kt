package dev.rubentxu.hodei.packages.domain.repository.merkle

import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleGraph
import dev.rubentxu.hodei.packages.domain.model.merkle.Signature

/**
 * Puerto para el acceso a grafos Merkle.
 * Define las operaciones para almacenar, recuperar y verificar grafos Merkle
 * que representan la estructura criptográfica de verificación de los artefactos.
 * 
 * Siguiendo los principios de la arquitectura hexagonal, esta interfaz actúa como
 * un puerto en el modelo de dominio que será implementado por adaptadores
 * en la capa de infraestructura.
 */
interface MerkleGraphRepository {
    /**
     * Guarda un grafo Merkle en el repositorio.
     * 
     * @param graph El grafo Merkle a guardar
     * @return Resultado encapsulando el grafo guardado o un error
     */
    suspend fun save(graph: MerkleGraph): Result<MerkleGraph>
    
    /**
     * Recupera un grafo Merkle por el ID del artefacto asociado.
     * 
     * @param artifactId El ID del artefacto
     * @return Resultado encapsulando el grafo encontrado (o null si no existe) o un error
     */
    suspend fun findByArtifactId(artifactId: String): Result<MerkleGraph?>
    
    /**
     * Recupera un grafo Merkle por el hash raíz.
     * 
     * @param rootHash El hash raíz que identifica unívocamente un estado del artefacto
     * @return Resultado encapsulando el grafo encontrado (o null si no existe) o un error
     */
    suspend fun findByRootHash(rootHash: ContentHash): Result<MerkleGraph?>
    
    /**
     * Añade una firma a un grafo Merkle existente.
     * 
     * @param artifactId El ID del artefacto cuyo grafo se va a firmar
     * @param signature La firma a añadir
     * @return Resultado encapsulando el grafo actualizado o un error
     */
    suspend fun addSignature(artifactId: String, signature: Signature): Result<MerkleGraph>
    
    /**
     * Verifica la validez estructural del grafo Merkle, recalculando todos los hashes
     * desde las hojas hasta la raíz y comprobando su consistencia.
     * 
     * @param artifactId El ID del artefacto a verificar
     * @return Resultado encapsulando true si el grafo es válido, false si no, o un error
     */
    suspend fun verifyGraphStructure(artifactId: String): Result<Boolean>
    
    /**
     * Lista todos los grafos Merkle asociados con un determinado keyId (firmante).
     * 
     * @param keyId El ID de la clave (normalmente email o nombre del firmante)
     * @return Resultado encapsulando la lista de grafos firmados por este keyId o un error
     */
    suspend fun findBySignatureKeyId(keyId: String): Result<List<MerkleGraph>>
}
