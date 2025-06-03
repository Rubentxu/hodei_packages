# Data Model: Modelo de Datos para PostgreSQL

Este documento describe el modelo de datos inicial para los metadatos que se almacenarán en PostgreSQL. Los artefactos binarios se almacenarán en el sistema de ficheros o S3.

## 1. Diagrama Entidad-Relación (ERD) - Conceptual

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar username UK "Nombre de usuario único"
        varchar hashed_password "Contraseña hasheada"
        varchar email UK "Email único"
        timestamp created_at "Fecha de creación"
        timestamp updated_at "Fecha de última actualización"
    }

    ROLES {
        int id PK
        varchar name UK "Nombre del rol (ej. admin, developer, reader)"
    }

    USER_ROLES {
        bigint user_id PK, FK "ID del usuario"
        int role_id PK, FK "ID del rol"
    }

    REPOSITORIES {
        bigint id PK
        varchar name UK "Nombre único del repositorio"
        varchar type "Tipo de repositorio (MAVEN, NPM, DOCKER, etc.)"
        jsonb configuration "Configuración específica del tipo (ej. proxy, políticas)"
        bigint storage_id FK "ID de la configuración de almacenamiento"
        timestamp created_at
        timestamp updated_at
    }

    STORAGES {
        bigint id PK
        varchar type "Tipo de almacenamiento (FILESYSTEM, S3)"
        jsonb configuration "Configuración (path, bucket_name, credentials_secret, etc.)"
    }

    ARTIFACTS {
        bigint id PK
        bigint repository_id FK "ID del repositorio al que pertenece"
        varchar path UK "Ruta única del artefacto dentro del repositorio (ej. org/group/artifact/version/file.jar)"
        varchar name "Nombre del archivo del artefacto"
        varchar content_type "MIME type del artefacto"
        bigint size_bytes "Tamaño en bytes"
        varchar checksum_sha256 "Checksum SHA256 del binario"
        varchar checksum_md5 "Checksum MD5 del binario (si aplica)"
        jsonb metadata "Metadatos específicos del tipo de artefacto (POM info, package.json info)"
        bigint uploaded_by_user_id FK "Usuario que subió el artefacto"
        timestamp uploaded_at "Fecha de subida"
        timestamp last_downloaded_at "Fecha de última descarga"
        bigint download_count "Contador de descargas"
        varchar binary_location "Referencia a la ubicación del binario (path en FS o key en S3)"
    }

    PERMISSIONS {
        bigint id PK
        bigint role_id FK "Rol al que se aplica el permiso"
        bigint repository_id FK "Repositorio específico (NULL si es global)"
        varchar action "Acción permitida (READ, WRITE, DELETE_ARTIFACT, ADMIN_REPO)"
    }

    AUDIT_LOGS {
        bigint id PK
        bigint user_id FK "Usuario que realizó la acción (NULL si es sistema)"
        varchar action "Acción realizada (ej. LOGIN, UPLOAD_ARTIFACT, CREATE_REPO)"
        jsonb details "Detalles de la acción (IP, parámetros, resultado)"
        timestamp occurred_at
    }

    USERS ||--o{ USER_ROLES : "tiene"
    ROLES ||--o{ USER_ROLES : "asociado a"
    USERS ||--o{ ARTIFACTS : "subido por"
    REPOSITORIES ||--o{ ARTIFACTS : "contiene"
    STORAGES ||--o{ REPOSITORIES : "usa para"
    ROLES ||--o{ PERMISSIONS : "tiene"
    REPOSITORIES ||--o{ PERMISSIONS : "aplica a"
    USERS ||--o{ AUDIT_LOGS : "realizado por"

```

## 2. Descripción de las Tablas Principales

*   **USERS:** Almacena la información de los usuarios del sistema.
*   **ROLES:** Define los roles disponibles en el sistema (ej. `admin`, `developer`, `reader`).
*   **USER_ROLES:** Tabla de unión para la relación muchos-a-muchos entre usuarios y roles.
*   **REPOSITORIES:** Define los repositorios de artefactos. Cada repositorio tiene un tipo (Maven, npm, etc.) y una configuración específica.
*   **STORAGES:** Define las configuraciones de almacenamiento para los binarios de los artefactos (ej. un path en el sistema de archivos local, o un bucket S3 con sus credenciales).
*   **ARTIFACTS:** Contiene los metadatos de cada artefacto almacenado, incluyendo su ruta, tamaño, checksums, quién lo subió y dónde está el binario.
*   **PERMISSIONS:** Define qué acciones puede realizar un rol sobre un repositorio específico (o globalmente si `repository_id` es NULL).
*   **AUDIT_LOGS:** Registra eventos importantes del sistema para auditoría.

## 3. Consideraciones

*   **Indexación:** Se crearán índices adecuados en columnas usadas frecuentemente en búsquedas (FKs, `username`, `email`, `repository.name`, `artifact.path`, etc.) para optimizar el rendimiento.
*   **Checksums:** Esenciales para verificar la integridad de los artefactos.
*   **Metadatos Específicos (`ARTIFACTS.metadata`):** El campo JSONB permitirá flexibilidad para almacenar metadatos diferentes según el tipo de artefacto (ej. dependencias de un POM, información de `package.json`).
*   **Configuración Flexible (`REPOSITORIES.configuration`, `STORAGES.configuration`):** Campos JSONB para configuraciones extensibles.
*   **Relaciones:** Las claves foráneas (FK) aseguran la integridad referencial.

Este modelo es inicial y evolucionará a medida que se desarrollen los casos de uso. Se utilizarán migraciones de base de datos (ej. Flyway, Liquibase) para gestionar los cambios de esquema.
