# Glossary: Términos Clave del Proyecto Hodei Packages

Este documento define los términos clave utilizados en el proyecto Hodei Packages para asegurar un entendimiento común.

*   **Artefacto (Artifact):**
    *   Unidad de software empaquetada y versionada, como una biblioteca JAR, un paquete NPM, una imagen Docker, etc. Es el objeto principal que gestiona el sistema.

*   **Repositorio (Repository):**
    *   Una colección nombrada de artefactos, generalmente de un tipo específico (ej. un repositorio Maven, un repositorio NPM). Puede tener políticas de acceso y configuración propias.
    *   Tipos de Repositorio:
        *   **Hosted:** Repositorio donde los artefactos son directamente subidos y almacenados por el sistema Hodei.
        *   **Proxy:** Repositorio que actúa como caché para un repositorio remoto (ej. Maven Central, npmjs.org).
        *   **Group:** Agregación de múltiples repositorios (hosted y/o proxy) bajo una única URL, simplificando la configuración del cliente.

*   **Metadatos (Metadata):**
    *   Información descriptiva sobre un artefacto (ej. `groupId`, `artifactId`, `version` para Maven; `name`, `version`, `dependencies` para NPM), su tamaño, checksums, fecha de subida, etc.

*   **Kotlin/Native:**
    *   Tecnología que compila código Kotlin directamente a ejecutables nativos sin necesidad de una JVM, buscando mejor rendimiento y menor consumo de memoria.

*   **Angular (19+):**
    *   Framework de desarrollo frontend basado en TypeScript para construir aplicaciones web SPA (Single Page Application).

*   **Arquitectura Hexagonal (Puertos y Adaptadores):**
    *   Patrón arquitectónico que aísla la lógica de negocio central (dominio) de las preocupaciones de infraestructura (UI, base de datos, mensajería) mediante el uso de puertos (interfaces) y adaptadores (implementaciones).

*   **Arquitectura Dirigida por Eventos (Event-Driven Architecture - EDA):**
    *   Patrón arquitectónico donde los componentes del sistema se comunican de forma asíncrona mediante la producción y consumo de eventos a través de un bus de mensajes.

*   **Control de Acceso Basado en Roles (RBAC - Role-Based Access Control):**
    *   Modelo de seguridad donde los permisos para realizar acciones se asignan a roles, y los usuarios heredan permisos a través de los roles que se les asignan.

*   **Monorepo:**
    *   Estrategia de control de versiones donde el código de múltiples proyectos (ej. backend, frontend, bibliotecas compartidas) se almacena en un único repositorio Git.

*   **Scaffolding:**
    *   Proceso de generar la estructura básica de directorios, archivos de configuración y código inicial para un proyecto o módulo.

*   **TDD (Test-Driven Development - Desarrollo Guiado por Pruebas):**
    *   Metodología de desarrollo donde se escriben pruebas (que inicialmente fallan) antes de escribir el código de producción para satisfacer esas pruebas, seguido de refactorización.

*   **CI/CD (Continuous Integration / Continuous Delivery or Deployment):**
    *   Prácticas para automatizar la integración de cambios de código, la construcción, las pruebas y el despliegue de software.

*   **Ktor:**
    *   Framework de Kotlin para construir aplicaciones asíncronas, incluyendo servidores HTTP y clientes. Considerado para el backend.

*   **Kotest:**
    *   Framework de pruebas para Kotlin, conocido por su DSL flexible y soporte para múltiples estilos de prueba.

*   **MockK:**
    *   Librería de mocking para Kotlin.

*   **PostgreSQL:**
    *   Sistema de gestión de bases de datos relacional de objetos, open source. Usado para almacenar metadatos.

*   **Apache Kafka / RabbitMQ:**
    *   Plataformas de streaming de eventos / brokers de mensajes, considerados para la EDA.

*   **Docker:**
    *   Plataforma de contenerización para empaquetar aplicaciones y sus dependencias.

*   **Kubernetes (K8s):**
    *   Sistema de orquestación de contenedores para automatizar el despliegue, escalado y gestión de aplicaciones contenerizadas.

*   **Helm:**
    *   Gestor de paquetes para Kubernetes, que facilita la definición, instalación y actualización de aplicaciones complejas en K8s.

*   **MVP (Minimum Viable Product - Producto Mínimo Viable):**
    *   Versión inicial del producto con suficientes características para satisfacer a los primeros usuarios y proporcionar retroalimentación para el desarrollo futuro.

Este glosario se expandirá a medida que surjan nuevos términos relevantes en el proyecto.
