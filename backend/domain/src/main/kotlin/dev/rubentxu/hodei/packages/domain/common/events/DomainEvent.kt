package dev.rubentxu.hodei.packages.domain.common.events

import java.time.Instant
import java.util.UUID

/**
 * Interfaz base para todos los eventos de dominio.
 * Define las propiedades comunes que deben tener todos los eventos del dominio.
 */
interface DomainEvent {
    /**
     * Identificador único del evento.
     */
    val eventId: String
    
    /**
     * Marca de tiempo que indica cuándo ocurrió el evento.
     */
    val timestamp: Instant
}

/**
 * Clase abstracta base que implementa propiedades comunes para eventos de dominio.
 * Proporciona implementaciones por defecto para eventId y timestamp.
 */
abstract class BaseDomainEvent : DomainEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Instant = Instant.now()
} 