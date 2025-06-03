package dev.rubentxu.hodei.packages.domain.model.merkle

/**
 * Representa un grafo Merkle completo para un artefacto.
 * Un grafo Merkle es una estructura de datos que permite verificar eficientemente
 * la integridad y autenticidad del contenido de un artefacto y sus metadatos.
 *
 * @param artifactId Identificador del artefacto al que pertenece este grafo
 * @param rootNode Nodo raíz del grafo que contiene la estructura completa de nodos hijos
 * @param signatures Lista de firmas que verifican la autenticidad del grafo (opcional)
 */
data class MerkleGraph(
    val artifactId: String,
    val rootNode: MerkleNode,
    val signatures: List<Signature> = emptyList()
) {
    /**
     * Hash criptográfico de la raíz del grafo, que representa el hash de todo el contenido
     * del artefacto. Este hash es el que se firma criptográficamente para verificación.
     */
    val rootHash: ContentHash = rootNode.contentHash

    init {
        require(artifactId.isNotBlank()) { "ArtifactId cannot be blank" }
    }

    /**
     * Añade una firma al grafo Merkle y devuelve una nueva instancia con la firma incluida.
     * La firma debe corresponder al hash raíz del grafo para ser válida.
     *
     * @param signature La firma a añadir
     * @return Una nueva instancia de MerkleGraph con la firma añadida
     * @throws IllegalArgumentException si la firma no corresponde al hash raíz
     */
    fun addSignature(signature: Signature): MerkleGraph {
        require(signature.contentHash == rootHash) { "Signature contentHash must match graph rootHash" }
        return copy(signatures = signatures + signature)
    }
    
    /**
     * Verifica que el grafo sea válido, es decir, que la estructura de hashes
     * sea consistente desde las hojas hasta la raíz.
     * 
     * @return true si el grafo es válido, false en caso contrario
     */
    fun isGraphValid(): Boolean {
        // Función recursiva que verifica la validez de un nodo y sus hijos
        fun validateNode(node: MerkleNode): Boolean {
            // Si es un nodo hoja (archivo), asumimos que es válido
            if (node.nodeType == MerkleNodeType.FILE) {
                return true
            }
            
            // Si es un directorio, verificamos que todos sus hijos sean válidos
            if (node.children.isEmpty()) {
                return false // Un nodo directorio debe tener hijos
            }
            
            // Verificar que todos los hijos son válidos
            val allChildrenValid = node.children.all { validateNode(it) }
            if (!allChildrenValid) {
                return false
            }
            
            // Calcular el hash esperado basado en los hashes de los hijos
            val expectedHash = MerkleNode.computeHash(
                path = node.path,
                children = node.children,
                algorithm = node.contentHash.algorithm
            ).contentHash
            
            // Verificar que el hash calculado coincida con el hash almacenado
            return node.contentHash == expectedHash
        }
        
        return validateNode(rootNode)
    }
}
