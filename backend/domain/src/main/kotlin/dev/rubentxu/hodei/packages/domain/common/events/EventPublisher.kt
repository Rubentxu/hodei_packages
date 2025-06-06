package dev.rubentxu.hodei.packages.domain.artifactmanagement.common.events

/**
 * Interfaz para la publicación de eventos de dominio.
 * Define un contrato que deben implementar los adaptadores de infraestructura
 * encargados de publicar eventos.
 */
interface EventPublisher {
    /**
     * Publica un evento de dominio.
     *
     * @param event El evento de dominio a publicar.
     */
    suspend fun publish(event: DomainEvent): Result<Unit>
} 