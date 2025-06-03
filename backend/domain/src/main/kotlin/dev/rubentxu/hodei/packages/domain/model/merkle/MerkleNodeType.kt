package dev.rubentxu.hodei.packages.domain.model.merkle

/**
 * Enumeración que representa los posibles tipos de nodos en un grafo Merkle.
 * Un nodo puede ser un archivo individual (FILE) o un directorio que contiene
 * otros nodos (DIRECTORY).
 */
enum class MerkleNodeType {
    FILE,
    DIRECTORY
}
