# System Patterns: Arquitectura y Diseño

## 1. Arquitectura General

El sistema adoptará una **Arquitectura Hexagonal (Puertos y Adaptadores)** combinada con un enfoque **Dirigido por Eventos (Event-Driven Architecture - EDA)**.

```mermaid
graph TD
    subgraph External_World [Mundo Exterior]
        UI[Usuario vía Frontend Angular]
        CLI[Herramientas CLI <br/> (Maven, npm, etc.)]
        CI_CD[Sistema CI/CD]
        AdminTool[Herramienta Admin (potencial)]
    end

    subgraph Hodei_System [Sistema Hodei Packages]
        direction LR
        APIGateway[API Gateway / Adaptador REST]
        
        subgraph Application_Core [Núcleo de la Aplicación (Hexágono)]
            direction LR
            DomainLogic[Lógica de Dominio <br/> (Entidades, Servicios de Dominio)]
            PortsInterfaces[Puertos (Interfaces)]
        end

        subgraph Adapters_Layer [Capa de Adaptadores]
            direction TB
            RestAdapter[Adaptador REST (Entrada)]
            PersistenceAdapter[Adaptador de Persistencia (Salida) <br/> (PostgreSQL, FileSystem, S3)]
            EventPublisherAdapter[Adaptador Publicador de Eventos (Salida) <br/> (Kafka/RabbitMQ)]
            EventConsumerAdapter[Adaptador Consumidor de Eventos (Entrada)]
            AuthAdapter[Adaptador de Autenticación/Autorización (Salida/Entrada)]
        end
    end

    UI -- HTTPS --> APIGateway
    CLI -- HTTPS --> APIGateway
    CI_CD -- HTTPS --> APIGateway
    AdminTool -- HTTPS --> APIGateway

    APIGateway -- Invoca --> PortsInterfaces
    PortsInterfaces -- Implementado por --> DomainLogic
    DomainLogic -- Usa --> PortsInterfaces

    RestAdapter -- Implementa Puerto Entrada para --> DomainLogic
    DomainLogic -- Usa Puerto Salida implementado por --> PersistenceAdapter
    DomainLogic -- Usa Puerto Salida implementado por --> EventPublisherAdapter
    EventConsumerAdapter -- Implementa Puerto Entrada para --> DomainLogic
    DomainLogic -- Usa Puerto Salida/Entrada implementado por --> AuthAdapter

    classDef hexagon fill:#D5F5E3,stroke:#1E8449,stroke-width:2px;
    classDef adapter fill:#EBF5FB,stroke:#2E86C1,stroke-width:2px;
    class Application_Core hexagon;
    class Adapters_Layer,APIGateway adapter;
```

**Descripción de la Arquitectura Hexagonal:**

*   **Núcleo de la Aplicación (Dominio):** Contiene la lógica de negocio pura (entidades, casos de uso/servicios de dominio). Es independiente de frameworks y tecnologías de infraestructura.
*   **Puertos:** Son interfaces definidas por el núcleo. Sirven como contratos para la comunicación.
    *   **Puertos de Entrada (Driving Ports):** Definen cómo el mundo exterior invoca la lógica de negocio (ej. `IArtifactService.uploadArtifact(...)`).
    *   **Puertos de Salida (Driven Ports):** Definen cómo el núcleo interactúa con servicios externos (ej. `IArtifactRepository.save(...)`, `IEventPublisher.publish(...)`).
*   **Adaptadores:** Son las implementaciones concretas de los puertos, conectando el núcleo con el mundo exterior.
    *   **Adaptadores de Entrada (Driving Adapters):** Convierten las solicitudes externas (ej. HTTP REST, mensajes de una cola) en llamadas a los puertos de entrada. Ej: Controladores REST, consumidores de Kafka.
    *   **Adaptadores de Salida (Driven Adapters):** Implementan los puertos de salida para interactuar con tecnologías específicas. Ej: Repositorios de base de datos (PostgreSQL), clientes S3, publicadores de Kafka.

**Arquitectura Dirigida por Eventos (EDA):**

Eventos de dominio significativos (ej. `ArtifactUploadedEvent`, `UserRegisteredEvent`, `RepositoryCreatedEvent`) se publicarán en un bus de mensajes (Kafka o RabbitMQ). Otros componentes del sistema o incluso sistemas externos pueden suscribirse a estos eventos para reaccionar de forma asíncrona y desacoplada.

*   **Beneficios:**
    *   **Desacoplamiento:** Los servicios no necesitan conocerse directamente.
    *   **Escalabilidad:** Diferentes partes del sistema pueden escalar independientemente.
    *   **Resiliencia:** Fallos en un consumidor no afectan directamente al productor del evento.
    *   **Extensibilidad:** Nuevos servicios pueden reaccionar a eventos existentes sin modificar el código original.

```mermaid
graph TD
    ServiceA[Servicio A (ej. Upload Artefacto)] -- Publica Evento --> EventBus[Bus de Mensajes (Kafka/RabbitMQ)]
    EventBus -- Evento X --> ServiceB[Servicio B (ej. Indexación)]
    EventBus -- Evento X --> ServiceC[Servicio C (ej. Notificaciones)]
    EventBus -- Evento Y --> ServiceD[Servicio D (ej. Auditoría)]
```

## 2. Patrones de Diseño Clave Adicionales

*   **Inyección de Dependencias (DI):** Se usará extensivamente (ej. Koin para Kotlin, DI de Angular) para gestionar dependencias y facilitar el testing.
*   **Repository Pattern:** Para abstraer el acceso a los datos de las entidades del dominio.
*   **Service Layer:** Para orquestar la lógica de los casos de uso.
*   **Data Transfer Objects (DTOs):** Para la comunicación entre capas, especialmente en la API REST.
*   **Unit of Work (Consideración):** Para gestionar transacciones que abarcan múltiples operaciones de repositorio, si es necesario.
*   **CQRS (Command Query Responsibility Segregation) (Consideración a futuro):** Si los modelos de lectura y escritura se vuelven muy diferentes y complejos, se podría evaluar para optimizar cada lado.

## 3. Decisiones de Diseño que Impactan la Testabilidad

*   La Arquitectura Hexagonal es inherentemente testable al aislar el dominio.
*   El uso de DI permite mockear/stubbear dependencias fácilmente en pruebas unitarias y de integración.
*   Las APIs REST bien definidas (OpenAPI) permitirán pruebas de contrato.
*   La EDA requerirá estrategias para probar componentes asíncronos (ej. Testcontainers para brokers de mensajes, mocks, o frameworks de prueba de mensajería).

Este documento informa `activeContext.md` y `techContext.md`.