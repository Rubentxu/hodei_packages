package dev.rubentxu.hodei.packages.domain.model.merkle

/**
 * Representa un nodo en el grafo Merkle.
 * Un nodo puede ser un archivo individual con su propio hash de contenido,
 * o un directorio que contiene otros nodos y cuyo hash se calcula a partir
 * de los hashes de sus hijos.
 *
 * @param path Ruta del nodo (relativa al artefacto)
 * @param contentHash Hash del contenido del nodo
 * @param nodeType Tipo de nodo (FILE o DIRECTORY)
 * @param children Lista de nodos hijos (para tipo DIRECTORY)
 */
data class MerkleNode(
    val path: String,
    val contentHash: ContentHash,
    val nodeType: MerkleNodeType = MerkleNodeType.FILE,
    val children: List<MerkleNode> = emptyList()
) {
    init {
        require(path.isNotBlank()) { "Node path cannot be blank" }
        
        // Si es un nodo de tipo DIRECTORY, debería tener hijos
        if (nodeType == MerkleNodeType.DIRECTORY) {
            require(children.isNotEmpty()) { "Directory node must have children" }
        }
    }

    companion object {
        /**
         * Crea un nodo de tipo DIRECTORY calculando su hash a partir de los hashes de sus hijos.
         *
         * @param path Ruta del nodo directorio
         * @param children Lista de nodos hijos
         * @param algorithm Algoritmo de hash a utilizar (por defecto SHA-256)
         * @return Un nuevo nodo MerkleNode de tipo DIRECTORY
         */
        fun computeHash(path: String, children: List<MerkleNode>, algorithm: String = "SHA-256"): MerkleNode {
            require(children.isNotEmpty()) { "Children list cannot be empty for hash computation" }
            
            // Concatenamos los hashes de todos los hijos en orden
            val combinedHashes = children.joinToString("") { it.contentHash.value }
            
            // Calculamos el hash del directorio basado en los hashes combinados de sus hijos
            val dirHash = ContentHash.create(combinedHashes, algorithm)
            
            return MerkleNode(
                path = path,
                contentHash = dirHash,
                nodeType = MerkleNodeType.DIRECTORY,
                children = children
            )
        }
    }
}
