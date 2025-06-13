# System Patterns: Hodei Packages

## 1. Arquitectura Principal: Hexagonal (Puertos y Adaptadores)

El sistema se diseña siguiendo una estricta **Arquitectura Hexagonal** para garantizar el desacoplamiento entre la lógica de negocio (dominio) y los detalles de infraestructura (frameworks, bases de datos).

- **Núcleo (Core)**:
  - **Dominio**: Contiene las entidades, agregados y lógica de negocio pura (ej. `Artifact`, `Repository`, `User`). No tiene dependencias externas.
  - **Aplicación**: Orquesta los casos de uso (servicios de aplicación). Define los **puertos** (interfaces) que necesita para interactuar con el exterior (ej. `ArtifactRepositoryPort`, `UserRepositoryPort`).

- **Infraestructura (Capa Exterior)**:
  - **Adaptadores de Entrada (Driving Adapters)**: Convierten las peticiones externas en llamadas a los servicios de aplicación. El principal adaptador de entrada es la **API REST con Ktor**, que traduce las peticiones HTTP en llamadas a los casos de uso.
  - **Adaptadores de Salida (Driven Adapters)**: Implementan los puertos definidos en la capa de aplicación. Ejemplos: un adaptador de persistencia que implementa `ArtifactRepositoryPort` usando una base de datos SQL, o un adaptador de sistema de archivos para almacenar los binarios de los artefactos.

```mermaid
graph TD
    subgraph Infraestructura
        A[API REST Ktor] -->|Llama a| B(Servicios de Aplicación)
        B -->|Usa| C{Puertos (Interfaces)}
        D[Adaptador BBDD] -->|Implementa| C
        E[Adaptador Ficheros] -->|Implementa| C
    end

    subgraph Núcleo
        B --> F(Dominio)
    end

    style F fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#ccf,stroke:#333,stroke-width:2px
```

## 2. Principios de Diseño

- **SOLID**: Los principios SOLID son la guía fundamental para el diseño de clases y componentes.
  - **SRP**: Cada clase (servicio de aplicación, adaptador) tiene una única responsabilidad.
  - **OCP**: La arquitectura de puertos y adaptadores facilita la extensión sin modificar el núcleo.
  - **LSP**: Las implementaciones de los puertos (adaptadores) deben ser sustituibles.
  - **ISP**: Los puertos se definen de forma granular para que los clientes no dependan de métodos que no usan.
  - **DIP**: Las capas internas (aplicación) dependen de abstracciones (puertos), no de concreciones (infraestructura).

- **Clean Code**: Se aplica un formato de código consistente, nombres descriptivos y funciones cortas y enfocadas.

## 3. Patrones de Diseño Clave

- **Inyección de Dependencias (DI)**: Se utilizará un framework de DI (como Koin o Kodein) para ensamblar la aplicación, inyectando los adaptadores en los servicios de aplicación y estos en la capa de API.
- **Repositorio**: Para abstraer el acceso a los datos de las entidades del dominio.
- **Servicio de Aplicación**: Para orquestar la lógica de los casos de uso.