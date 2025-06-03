# Implementación del Dominio para el Sistema de Repositorio de Artefactos

## Contexto General

El proyecto Hodei Packages es un sistema de repositorio de artefactos de alto rendimiento implementado en Kotlin, diseñado para almacenar, gestionar y servir artefactos de software (bibliotecas, paquetes) de forma eficiente y escalable. Sigue una arquitectura hexagonal (puertos y adaptadores) y está dirigido por eventos (Event-Driven).

## Enfoque del Dominio

Siguiendo los principios de la arquitectura hexagonal, el módulo de dominio es el núcleo puro de la aplicación, libre de dependencias externas y contiene:

1. **Entidades y Modelos de Dominio**: Representan los conceptos centrales del negocio
2. **Puertos (Interfaces)**: Definen contratos para los adaptadores externos
3. **Eventos de Dominio**: Para la comunicación basada en eventos entre componentes
4. **Servicios de Dominio**: Encapsulan la lógica de negocio compleja
5. **Excepciones de Dominio**: Errores específicos del dominio

## Modelos de Dominio Principales

### Repositorios y Artefactos (Fase 1)

- **Repository**: Entidad que representa un repositorio de artefactos con tipo específico (Maven, NPM)
  - Propiedades: id, nombre, tipo, descripción, visibilidad, timestamps
  - Validaciones: formato de nombre, longitud de descripción

- **Artifact**: Representación de paquetes almacenados con sus metadatos
  - Propiedades: id, repositoryId, groupId, artifactId, version, tamaño, hash, timestamps
  - Validaciones: formato de versión semántica, integridad (hash)

- **RBAC (Control de Acceso)**: Sistema de permisos basados en roles
  - Roles: Admin, Contributor, Reader (globales y por repositorio)
  - Permisos: CREATE_REPO, DELETE_REPO, UPLOAD_ARTIFACT, etc.
  - Validación: reglas de acceso a operaciones

### Modelos para Fases Posteriores

- **Políticas de Retención**: Reglas para mantener o eliminar versiones
  - Estrategias: por fecha, número de versiones, patrones de versión
  - Automatización: limpieza programada

- **Estadísticas y Auditoría**: Seguimiento de uso y actividad
  - Descargas, cargas, espacio utilizado
  - Historial de cambios por usuario

- **Formateo Específico**: Configuración por tipo de repositorio
  - Maven: políticas de snapshot, layouts
  - NPM: configuración de scopes

## Estructura del Dominio

```
domain/
├── model/              # Entidades de dominio
│   ├── repository/     # Modelos de repositorios
│   │   ├── Repository.kt
│   │   └── RepositoryType.kt
│   ├── artifact/       # Modelos de artefactos
│   │   └── Artifact.kt
│   ├── permission/     # Modelos de permisos y roles
│   │   ├── Permission.kt
│   │   ├── Role.kt
│   │   └── UserPermission.kt
│   └── stats/          # Modelos para estadísticas
├── repository/         # Puertos (interfaces)
│   ├── RepositoryRepository.kt
│   ├── ArtifactRepository.kt
│   └── PermissionRepository.kt
├── service/           # Servicios de dominio
│   ├── RepositoryService.kt
│   ├── ArtifactService.kt
│   └── RetentionService.kt
├── events/            # Eventos de dominio
│   ├── repository/
│   │   └── RepositoryEvent.kt
│   ├── artifact/
│   │   └── ArtifactEvent.kt
│   └── permission/
│       └── PermissionEvent.kt
└── exception/         # Excepciones específicas
    ├── InvalidArtifactException.kt
    └── PermissionDeniedException.kt
```

## Principios Clave para la Implementación

1. **Pureza del Dominio**: No depende de frameworks externos ni infraestructura
2. **Inmutabilidad**: Estructuras inmutables (data classes) para garantizar integridad
3. **Expresividad**: Código que refleja el lenguaje del dominio (DDD)
4. **Eventos para Cambios de Estado**: Comunicación de cambios importantes mediante eventos
5. **Interfaces Claras**: Puertos con contratos explícitos para la comunicación externa
6. **Validaciones Intrínsecas**: Las entidades validan su propio estado
7. **Alto Rendimiento**: Diseño optimizado para operaciones de alta frecuencia

## Implementación en Progreso

1. **Repositorios**:
   - ✅ Modelo Repository con validaciones
   - ✅ Enum RepositoryType (MAVEN, NPM)
   - ✅ Puerto RepositoryRepository
   - ✅ Eventos de dominio para repositorios

2. **Artefactos**:
   - ✅ Modelo Artifact con validación semántica
   - ✅ Puerto ArtifactRepository
   - ✅ Eventos de dominio para artefactos

3. **RBAC** (en progreso):
   - 🔄 Modelos Permission y Role
   - 🔄 Puerto PermissionRepository
   - 🔄 Eventos de cambio de permisos

## Próximos Pasos

1. Completar el modelo RBAC para control de acceso granular
2. Implementar servicios de dominio para operaciones complejas
3. Crear políticas de retención y gestión de versiones
4. Definir modelo para estadísticas y métricas
5. Implementar validaciones específicas por tipo de repositorio

## Consideraciones de Rendimiento

Aunque el dominio es independiente de la implementación, se diseña considerando:

- Operaciones optimizadas para alta concurrencia
- Modelos que faciliten cacheo eficiente
- Eventos que permitan procesamiento asíncrono
- Estructuras adecuadas para búsqueda y recuperación rápida