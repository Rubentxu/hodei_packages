package dev.rubentxu.hodei.packages.domain.integrityverification.merkle.events


import dev.rubentxu.hodei.packages.domain.artifactmanagement.common.events.DomainEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.ContentHash
import dev.rubentxu.hodei.packages.domain.integrityverification.merkle.model.Signature
import java.time.Instant
import java.util.*

/**
 * Eventos de dominio relacionados con operaciones de grafos Merkle y verificación criptográfica.
 * Estos eventos permiten la comunicación asíncrona entre componentes del sistema
 * cuando ocurren acciones relacionadas con verificación e integridad de artefactos.
 */

/**
 * Evento base para todas las operaciones relacionadas con grafos Merkle.
 */
sealed class MerkleEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Evento emitido cuando se genera un nuevo grafo Merkle para un artefacto.
 */
data class MerkleGraphGeneratedEvent(
    val artifactId: String,
    val rootHash: ContentHash,
    val totalNodes: Int
) : MerkleEvent()

/**
 * Evento emitido cuando se firma un grafo Merkle.
 */
data class MerkleGraphSignedEvent(
    val artifactId: String,
    val rootHash: ContentHash,
    val signature: Signature,
    val keyId: String
) : MerkleEvent()

/**
 * Evento emitido cuando se verifica exitosamente un grafo Merkle y sus firmas.
 */
data class MerkleGraphVerifiedEvent(
    val artifactId: String,
    val rootHash: ContentHash,
    val verifiedSignatures: List<String> // Lista de keyIds de las firmas verificadas
) : MerkleEvent()

/**
 * Evento emitido cuando la verificación de un grafo Merkle falla.
 * Esto puede ser por inconsistencia en la estructura de hashes o por firmas inválidas.
 */
data class MerkleVerificationFailedEvent(
    val artifactId: String,
    val rootHash: ContentHash?,
    val reason: String,
    val failureLocation: String? = null, // Ruta del nodo donde falló la verificación, si aplica
    val errorDetails: Map<String, String> = emptyMap()
) : MerkleEvent()

/**
 * Evento emitido cuando se detecta una modificación no autorizada en un artefacto.
 * Esto ocurre cuando el hash actual de algún componente no coincide con el registrado en el grafo.
 */
data class TamperingDetectedEvent(
    val artifactId: String,
    val expectedHash: ContentHash,
    val actualHash: ContentHash,
    val nodePath: String,
    val detectedAt: Instant = Instant.now()
) : MerkleEvent()

/**
 * Evento emitido cuando se genera una prueba criptográfica que demuestra la integridad
 * y autenticidad de un artefacto.
 */
data class CryptographicProofGeneratedEvent(
    val artifactId: String,
    val rootHash: ContentHash,
    val proofId: String,
    val verifierUrls: List<String> = emptyList() // URLs donde esta prueba puede ser verificada
) : MerkleEvent()
