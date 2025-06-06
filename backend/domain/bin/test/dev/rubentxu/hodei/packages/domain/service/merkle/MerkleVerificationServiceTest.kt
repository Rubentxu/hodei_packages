package dev.rubentxu.hodei.packages.domain.service.merkle

import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.MerkleGraphVerifiedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.MerkleVerificationFailedEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events.TamperingDetectedEvent
import dev.rubentxu.hodei.packages.domain.model.merkle.ContentHash
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleGraph
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNode
import dev.rubentxu.hodei.packages.domain.model.merkle.MerkleNodeType
import dev.rubentxu.hodei.packages.domain.model.merkle.Signature
import dev.rubentxu.hodei.packages.domain.ports.merkle.ContentAddressableStorage
import dev.rubentxu.hodei.packages.domain.ports.merkle.CryptographicService
import dev.rubentxu.hodei.packages.domain.ports.merkle.MerkleGraphRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot

class MerkleVerificationServiceTest : StringSpec({
    
    val mockEventPublisher = mockk<EventPublisher>(relaxed = true)
    val mockMerkleGraphRepository = mockk<MerkleGraphRepository>()
    val mockCryptographicService = mockk<CryptographicService>()
    val mockContentStorage = mockk<ContentAddressableStorage>()
    
    // Usaremos slots individuales para cada test en lugar de listas compartidas
    // Esto asegura que cada test tenga su propio contexto aislado
    
    val verificationService = MerkleVerificationService(
        eventPublisher = mockEventPublisher,
        merkleGraphRepository = mockMerkleGraphRepository,
        cryptographicService = mockCryptographicService,
        contentStorage = mockContentStorage
    )
    
    val artifactId = "test-artifact-123"
    val contentHash = ContentHash("SHA-256", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    val fileNode = MerkleNode(path = "file.txt", contentHash = contentHash, nodeType = MerkleNodeType.FILE, children = emptyList())
    val rootNode = MerkleNode(path = "root", contentHash = ContentHash("SHA-256", "root-hash"), nodeType = MerkleNodeType.DIRECTORY, children = listOf(fileNode))
    
    val keyId = "user@example.com"
    val signature = Signature(
        value = "test-signature",
        algorithm = "Ed25519",
        contentHash = rootNode.contentHash,
        keyId = keyId
    )
    
    val merkleGraph = MerkleGraph(
        artifactId = artifactId,
        rootNode = rootNode,
        signatures = listOf(signature)
    )
    
    "verifyArtifact should return true for valid graph structure and signatures" {
        // Configuración del mock para encontrar el grafo
        coEvery { mockMerkleGraphRepository.findByArtifactId(artifactId) } returns Result.success(merkleGraph)
        
        // El grafo es estructuralmente válido
        coEvery { mockMerkleGraphRepository.verifyGraphStructure(artifactId) } returns Result.success(true)
        
        // La firma es válida
        coEvery { 
            mockCryptographicService.verify(signature, rootNode.contentHash, null)
        } returns Result.success(true)
        
        // Verificación exitosa
        val result = verificationService.verifyArtifact(artifactId)
        
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe true
        
        // Verificar publicación del evento de verificación exitosa
        val verifiedEventSlot = slot<MerkleGraphVerifiedEvent>()
        coVerify { 
            mockEventPublisher.publish(
                capture(verifiedEventSlot)
            ) 
        }
        
        verifiedEventSlot.captured.artifactId shouldBe artifactId
        verifiedEventSlot.captured.rootHash shouldBe rootNode.contentHash
        verifiedEventSlot.captured.verifiedSignatures shouldBe listOf(keyId)
    }
    
    "verifyArtifact should return false for invalid graph structure" {
        // Configuración del mock para encontrar el grafo
        coEvery { mockMerkleGraphRepository.findByArtifactId(artifactId) } returns Result.success(merkleGraph)
        
        // El grafo NO es estructuralmente válido
        coEvery { mockMerkleGraphRepository.verifyGraphStructure(artifactId) } returns Result.success(false)
        
        // Verificación fallida
        val result = verificationService.verifyArtifact(artifactId)
        
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe false
        
        // Verificar publicación del evento de fallo de verificación
        val failedStructureEventSlot = slot<MerkleVerificationFailedEvent>()
        coVerify { 
            mockEventPublisher.publish(
                capture(failedStructureEventSlot)
            ) 
        }
        
        failedStructureEventSlot.captured.artifactId shouldBe artifactId
        failedStructureEventSlot.captured.rootHash shouldBe rootNode.contentHash
        failedStructureEventSlot.captured.reason shouldBe "Invalid Merkle graph structure"
    }
    
    "verifyArtifact should return false for invalid signature" {
        // Configuración del mock para encontrar el grafo
        coEvery { mockMerkleGraphRepository.findByArtifactId(artifactId) } returns Result.success(merkleGraph)
        
        // El grafo es estructuralmente válido
        coEvery { mockMerkleGraphRepository.verifyGraphStructure(artifactId) } returns Result.success(true)
        
        // Pero la firma NO es válida
        coEvery { 
            mockCryptographicService.verify(signature, rootNode.contentHash, null)
        } returns Result.success(false)
        
        // Verificación fallida
        val result = verificationService.verifyArtifact(artifactId)
        
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe false
        
        // Verificar publicación del evento de fallo de verificación
        // Usamos un enfoque diferente para evitar conflictos con los slots
        coVerify { 
            mockEventPublisher.publish(
                match<MerkleVerificationFailedEvent> { event ->
                    event.artifactId == artifactId &&
                        event.rootHash == rootNode.contentHash &&
                        event.reason == "Invalid signature"
                }
            ) 
        }
    }
    
    "verifyArtifactContent should detect tampering when content hash is different" {
        // Mock para el ContentAddressableStorage
        val contentBytes = "modified content".toByteArray()
        val actualHash = ContentHash.create(contentBytes)
        
        // El hash actual es diferente al esperado
        coEvery { 
            mockContentStorage.retrieve(any())
        } returns Result.success(contentBytes)
        
        // Configuración del mock para encontrar el grafo
        coEvery { 
            mockMerkleGraphRepository.findByArtifactId(artifactId) 
        } returns Result.success(merkleGraph)
        
        // Verificación detecta manipulación
        val result = verificationService.verifyArtifactContent(artifactId, "file.txt")
        
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe false
        
        // Verificar publicación del evento de detección de manipulación
        val tamperingEventSlot = slot<TamperingDetectedEvent>()
        coVerify { 
            mockEventPublisher.publish(
                capture(tamperingEventSlot)
            ) 
        }
        
        tamperingEventSlot.captured.artifactId shouldBe artifactId
        tamperingEventSlot.captured.expectedHash shouldBe contentHash
        tamperingEventSlot.captured.nodePath shouldBe "file.txt"
        // No podemos verificar actualHash exactamente ya que depende de la implementación interna
    }
    
    "verifyArtifactContent should return true when content matches expected hash" {
        // Primero, creamos un hash real calculado con ContentHash.create para un contenido conocido
        val testContent = "test content for hash".toByteArray()
        val algorithm = "SHA-256"
        
        // Ahora usamos el hash real calculado como el hash esperado
        val expectedHash = ContentHash.create(testContent, algorithm)
        
        // Setup
        val artifactId = "test-artifact"
        val filePath = "file.txt"
        
        // Creamos un nodo de archivo con el hash esperado
        val fileNode = MerkleNode(
            path = filePath,
            contentHash = expectedHash,
            nodeType = MerkleNodeType.FILE,
            children = emptyList()
        )
        
        // Creamos un nodo raíz que contiene el archivo
        val rootHash = ContentHash(algorithm, "root123")
        val rootNode = MerkleNode(
            path = "root",
            contentHash = rootHash,
            nodeType = MerkleNodeType.DIRECTORY,
            children = listOf(fileNode)
        )
        
        val merkleGraph = MerkleGraph(rootNode = rootNode, artifactId = artifactId)
        
        // Lo importante aquí: configuramos el mock para que devuelva EXACTAMENTE el mismo contenido
        // que usamos para generar el hash, así cuando ContentHash.create() se ejecute en el servicio,
        // producirá el mismo hash que esperamos
        coEvery {
            mockContentStorage.retrieve(expectedHash)
        } returns Result.success(testContent)
        
        // Configuración del mock para encontrar el grafo
        coEvery { 
            mockMerkleGraphRepository.findByArtifactId(artifactId) 
        } returns Result.success(merkleGraph)
        
        // Act - usamos el servicio normal, no necesitamos instrumentarlo
        val result = verificationService.verifyArtifactContent(artifactId, filePath)
        
        // Assert - la verificación debería pasar ya que los hashes coinciden
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe true
    }
})
