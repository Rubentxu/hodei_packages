package dev.rubentxu.hodei.packages.domain.service.merkle

import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.events.merkle.MerkleGraphVerifiedEvent
import dev.rubentxu.hodei.packages.domain.events.merkle.MerkleVerificationFailedEvent
import dev.rubentxu.hodei.packages.domain.events.merkle.TamperingDetectedEvent
import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleGraph
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNode
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNodeType
import dev.rubentxu.hodei.packages.domain.repository.merkle.ContentAddressableStorage
import dev.rubentxu.hodei.packages.domain.repository.merkle.CryptographicService
import dev.rubentxu.hodei.packages.domain.repository.merkle.MerkleGraphRepository
import java.time.Instant

/**
 * Servicio del dominio responsable de la verificación de integridad y autenticidad de artefactos
 * utilizando grafos Merkle y firmas criptográficas.
 *
 * @param eventPublisher Publicador de eventos del dominio
 * @param merkleGraphRepository Repositorio para acceder a los grafos Merkle almacenados
 * @param cryptographicService Servicio para operaciones criptográficas
 * @param contentStorage Almacenamiento direccionable por contenido
 */
class MerkleVerificationService(
    private val eventPublisher: EventPublisher,
    private val merkleGraphRepository: MerkleGraphRepository,
    private val cryptographicService: CryptographicService,
    private val contentStorage: ContentAddressableStorage
) {
    /**
     * Verifica la integridad y autenticidad de un artefacto completo.
     * Comprueba tanto la validez estructural del grafo Merkle como
     * la validez de las firmas.
     *
     * @param artifactId ID del artefacto a verificar
     * @return Resultado encapsulando true si es válido, false si no, o un error
     */
    suspend fun verifyArtifact(artifactId: String): Result<Boolean> {
        return try {
            // Recuperar el grafo Merkle del artefacto
            val graphResult = merkleGraphRepository.findByArtifactId(artifactId)
            if (graphResult.isFailure) {
                return Result.failure(
                    graphResult.exceptionOrNull() ?: 
                    IllegalStateException("Failed to retrieve Merkle graph for artifact $artifactId")
                )
            }
            
            val graph = graphResult.getOrNull()
            if (graph == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = null,
                    reason = "Merkle graph not found for artifact"
                )
                return Result.success(false)
            }
            
            // Verificar la estructura del grafo
            val structureResult = merkleGraphRepository.verifyGraphStructure(artifactId)
            if (structureResult.isFailure) {
                return Result.failure(
                    structureResult.exceptionOrNull() ?: 
                    IllegalStateException("Error during graph structure verification")
                )
            }
            
            val isStructureValid = structureResult.getOrNull() ?: false
            if (!isStructureValid) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Invalid Merkle graph structure"
                )
                return Result.success(false)
            }
            
            // Verificar firmas
            val verifiedSignatures = mutableListOf<String>()
            var allSignaturesValid = true
            
            for (signature in graph.signatures) {
                val signatureResult = cryptographicService.verify(
                    signature = signature,
                    contentHash = graph.rootHash
                )
                
                if (signatureResult.isFailure) {
                    return Result.failure(
                        signatureResult.exceptionOrNull() ?: 
                        IllegalStateException("Error during signature verification")
                    )
                }
                
                val isSignatureValid = signatureResult.getOrNull() ?: false
                if (isSignatureValid) {
                    verifiedSignatures.add(signature.keyId)
                } else {
                    allSignaturesValid = false
                }
            }
            
            if (!allSignaturesValid || verifiedSignatures.isEmpty()) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Invalid signature"
                )
                return Result.success(false)
            }
            
            // Si llegamos aquí, todo es válido
            eventPublisher.publish(MerkleGraphVerifiedEvent(
                artifactId = artifactId,
                rootHash = graph.rootHash,
                verifiedSignatures = verifiedSignatures
            ))
            
            Result.success(true)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verifica la integridad de un archivo específico dentro de un artefacto.
     * Compara el hash actual del contenido con el hash registrado en el grafo Merkle.
     *
     * @param artifactId ID del artefacto que contiene el archivo
     * @param filePath Ruta del archivo a verificar
     * @return Resultado encapsulando true si es válido, false si no, o un error
     */
    suspend fun verifyArtifactContent(artifactId: String, filePath: String): Result<Boolean> {
        return try {
            // Recuperar el grafo Merkle
            val graphResult = merkleGraphRepository.findByArtifactId(artifactId)
            if (graphResult.isFailure || graphResult.getOrNull() == null) {
                return Result.failure(
                    graphResult.exceptionOrNull() ?: 
                    IllegalStateException("Failed to retrieve Merkle graph for artifact $artifactId")
                )
            }
            
            val graph = graphResult.getOrNull()!!
            
            // Buscar el nodo correspondiente a la ruta del archivo
            val fileNode = findNodeByPath(graph.rootNode, filePath)
            if (fileNode == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "File not found in Merkle graph",
                    failureLocation = filePath
                )
                return Result.success(false)
            }
            
            // Obtener el contenido actual del archivo
            val expectedHash = fileNode.contentHash
            val contentResult = contentStorage.retrieve(expectedHash)
            
            if (contentResult.isFailure) {
                return Result.failure(
                    contentResult.exceptionOrNull() ?: 
                    IllegalStateException("Failed to retrieve content for verification")
                )
            }
            
            val content = contentResult.getOrNull()
            if (content == null) {
                publishVerificationFailed(
                    artifactId = artifactId,
                    rootHash = graph.rootHash,
                    reason = "Content not found in storage",
                    failureLocation = filePath
                )
                return Result.success(false)
            }
            
            // Calcular el hash actual del contenido
            val actualHash = ContentHash.create(content, algorithm = expectedHash.algorithm)
            
            // Comparar hashes
            val isValid = actualHash == expectedHash
            
            if (!isValid) {
                // Detectada manipulación
                eventPublisher.publish(TamperingDetectedEvent(
                    artifactId = artifactId,
                    expectedHash = expectedHash,
                    actualHash = actualHash,
                    nodePath = filePath
                ))
            }
            
            Result.success(isValid)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Busca un nodo en el grafo Merkle por su ruta.
     */
    private fun findNodeByPath(node: MerkleNode, path: String): MerkleNode? {
        if (node.path == path) {
            return node
        }
        
        if (node.nodeType == MerkleNodeType.DIRECTORY) {
            for (child in node.children) {
                val result = findNodeByPath(child, path)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
    
    /**
     * Publica un evento de fallo de verificación.
     */
    private suspend fun publishVerificationFailed(
        artifactId: String,
        rootHash: ContentHash?,
        reason: String,
        failureLocation: String? = null
    ) {
        eventPublisher.publish(MerkleVerificationFailedEvent(
            artifactId = artifactId,
            rootHash = rootHash,
            reason = reason,
            failureLocation = failureLocation
        ))
    }
}
