package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.events


import dev.rubentxu.hodei.packages.domain.artifactmanagement.common.events.DomainEvent
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomDocument
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomFormat
import java.time.Instant
import java.util.UUID

/**
 * Eventos de dominio relacionados con operaciones de SBOM.
 * Estos eventos permiten la comunicación asíncrona entre componentes del sistema
 * cuando ocurren acciones relacionadas con documentos SBOM.
 */

/**
 * Evento base para todas las operaciones relacionadas con SBOM.
 */
sealed class SbomEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Evento emitido cuando se inicia la generación de un documento SBOM para un artefacto.
 */
data class SbomGenerationStartedEvent(
    val artifactId: String,
    val format: SbomFormat,
    val triggeredBy: String? = null // Usuario o sistema que inició la generación
) : SbomEvent()

/**
 * Evento emitido cuando se completa la generación de un documento SBOM.
 */
data class SbomGenerationCompletedEvent(
    val sbomDocument: SbomDocument,
    val artifactId: String,
    val durationMs: Long
) : SbomEvent()

/**
 * Evento emitido cuando falla la generación de un documento SBOM.
 */
data class SbomGenerationFailedEvent(
    val artifactId: String,
    val format: SbomFormat,
    val errorMessage: String,
    val errorDetails: Map<String, String> = emptyMap()
) : SbomEvent()

/**
 * Evento emitido cuando se guarda un nuevo documento SBOM en el sistema.
 */
data class SbomStoredEvent(
    val sbomDocument: SbomDocument
) : SbomEvent()

/**
 * Evento emitido cuando se inicia el análisis de un documento SBOM para
 * buscar vulnerabilidades u otros problemas.
 */
data class SbomAnalysisStartedEvent(
    val sbomId: String,
    val artifactId: String,
    val analysisType: String // Por ejemplo: "vulnerability", "license", "dependency"
) : SbomEvent()

/**
 * Evento emitido cuando se completa el análisis de un documento SBOM.
 */
data class SbomAnalysisCompletedEvent(
    val sbomId: String,
    val artifactId: String,
    val analysisType: String,
    val issues: Int,
    val summaryData: Map<String, Object> = emptyMap()
) : SbomEvent()
