# Tech Context: Stack Tecnológico y Configuración

## 1. Tecnologías Principales

*   **Backend:**
    *   Lenguaje: **Kotlin** (orientado a **Kotlin/Native** para compilación nativa donde sea posible y beneficioso, o JVM si es necesario para ciertas librerías críticas).
    *   Framework API REST: **Ktor** (ligero, idiomático para Kotlin, buen soporte para corutinas y potencial para Kotlin/Native).
    *   Persistencia de Metadatos: **PostgreSQL** (versión 15+).
        *   Acceso a Datos: **Exposed SQL Framework** (DSL de Kotlin para SQL) o **JPA/Hibernate** (si se opta por JVM y se requiere un ORM completo).
    *   Almacenamiento de Binarios: Sistema de ficheros local, **Amazon S3** (o compatible).
    *   Mensajería (Event-Driven): **Apache Kafka** (preferido por escalabilidad) o **RabbitMQ**.
    *   Contenerización: **Docker**.
*   **Frontend:**
    *   Framework: **Angular 19+** (TypeScript).
    *   Gestor de Estado: **NgRx** (para aplicaciones complejas) o servicios de Angular con RxJS para casos más simples.
    *   UI Components: **Angular Material** o una librería de componentes custom.
    *   Contenerización: **Docker** (con Nginx para servir estáticos).
*   **Orquestación de Contenedores:** **Kubernetes**.
*   **Build Tools:**
    *   Backend (Kotlin): **Gradle** con el plugin de Kotlin.
    *   Frontend (Angular): **Angular CLI** (basado en npm/yarn).
*   **Control de Versiones:** **Git** (ej. en GitHub, GitLab).

## 2. Frameworks y Herramientas de Pruebas

*   **Backend (Kotlin):**
    *   Pruebas Unitarias y de Integración: **Kotest** (DSL expresivo, multiplatforma).
    *   Mocking: **MockK**.
    *   Pruebas de API (End-to-End ligeras): Cliente HTTP de Ktor dentro de Kotest, o **RestAssured** si se usa JVM.
    *   Testcontainers: Para pruebas de integración con PostgreSQL, Kafka, etc.
*   **Frontend (Angular):**
    *   Pruebas Unitarias: **Jasmine** y **Karma** (vía Angular CLI).
    *   Pruebas de Componentes: Angular Testing Library o utilidades de Angular.
    *   Pruebas End-to-End (E2E): **Playwright** o **Cypress**.
*   **Pruebas de Contrato (API):** (Opcional) Pact.

## 3. Configuración del Entorno de Desarrollo

*   **IDEs:**
    *   Backend: **IntelliJ IDEA Ultimate** (con soporte para Kotlin, Gradle, Docker, Kubernetes).
    *   Frontend: **Visual Studio Code** (con plugins para Angular, TypeScript, Prettier, ESLint).
*   **Linters y Formateadores:**
    *   Kotlin: **ktlint**.
    *   TypeScript/Angular: **ESLint**, **Prettier**.
*   **Gestión de Dependencias:**
    *   Backend: Gradle.
    *   Frontend: npm o yarn.

## 4. Pipeline CI/CD (Conceptual)

```mermaid
graph TD
    A[Commit a Git] --> B{Webhook Trigger};
    B -- main/develop branch --> C[Pipeline CI/CD Inicia];
    
    subgraph Stages
        direction LR
        C --> D[1. Checkout & Setup];
        D --> BE_Build[2a. Backend: Build & Unit Test];
        D --> FE_Build[2b. Frontend: Build & Unit Test];
        
        BE_Build --> BE_Package[3a. Backend: Package (JAR/Native + Docker Image)];
        FE_Build --> FE_Package[3b. Frontend: Package (Static + Docker Image)];
        
        BE_Package --> BE_Push[4a. Backend: Push Image to Registry];
        FE_Package --> FE_Push[4b. Frontend: Push Image to Registry];
        
        BE_Push --> IntegrationTest[5. Deploy to Test Env & Run Integration/E2E Tests];
        FE_Push --> IntegrationTest;
        
        IntegrationTest -- Success --> StagingDeploy[6. Deploy to Staging (Optional)];
        IntegrationTest -- Failure --> NotifyFail[Notificar Fallo];
        
        StagingDeploy --> ProdApproval{7. Manual Approval for Prod};
        ProdApproval -- Approved --> ProdDeploy[8. Deploy to Production];
        ProdApproval -- Rejected --> NotifyReject[Notificar Rechazo];
    end

    classDef trigger fill:#FFD700,stroke:#333,stroke-width:2px;
    classDef build fill:#ADD8E6,stroke:#333,stroke-width:2px;
    classDef deploy fill:#90EE90,stroke:#333,stroke-width:2px;
    classDef test fill:#FFB6C1,stroke:#333,stroke-width:2px;

    class B,C trigger;
    class D,BE_Build,FE_Build,BE_Package,FE_Package,BE_Push,FE_Push build;
    class IntegrationTest test;
    class StagingDeploy,ProdDeploy deploy;
```

*   **Herramientas CI/CD:** Jenkins, GitLab CI, GitHub Actions.

## 5. Consideraciones Técnicas Adicionales

*   **Kotlin/Native:** Evaluar cuidadosamente la madurez y disponibilidad de librerías para Kotlin/Native (ej. clientes Kafka, drivers de base de datos). Si hay bloqueos, se podría optar por Kotlin/JVM para módulos específicos o para todo el backend inicialmente.
*   **Monorepo:** Se utilizará una estructura de monorepo (ej. gestionado con Gradle para el backend y npm/yarn workspaces para el frontend, o herramientas como Nx).

Este documento es una guía y puede evolucionar.
