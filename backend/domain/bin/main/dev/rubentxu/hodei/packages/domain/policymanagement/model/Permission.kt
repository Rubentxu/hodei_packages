package dev.rubentxu.hodei.packages.domain.policymanagement.model

/**
 * Enumeración que define los permisos disponibles en el sistema.
 * Cada permiso representa una acción específica que puede realizarse sobre un recurso.
 */
enum class Permission {
    // Permisos globales del sistema
    ADMIN_ACCESS,             // Acceso a funciones administrativas
    
    // Permisos de repositorios
    CREATE_REPOSITORY,        // Crear nuevos repositorios
    VIEW_REPOSITORY,          // Ver información básica de repositorios
    EDIT_REPOSITORY,          // Modificar configuración de repositorios
    DELETE_REPOSITORY,        // Eliminar repositorios
    
    // Permisos de artefactos
    UPLOAD_ARTIFACT,          // Subir nuevos artefactos
    DOWNLOAD_ARTIFACT,        // Descargar artefactos
    DELETE_ARTIFACT,          // Eliminar artefactos
    UPDATE_ARTIFACT_METADATA, // Actualizar metadatos de artefactos
    
    // Permisos de usuarios y roles
    MANAGE_USERS,             // Gestionar usuarios
    MANAGE_ROLES,             // Gestionar roles y permisos
    
    // Permisos de políticas
    MANAGE_RETENTION_POLICIES; // Gestionar políticas de retención
    
    companion object {
        /**
         * Permisos asignados al rol de administrador global.
         */
        val ADMIN_PERMISSIONS = setOf(
            ADMIN_ACCESS,
            CREATE_REPOSITORY, VIEW_REPOSITORY, EDIT_REPOSITORY, DELETE_REPOSITORY,
            UPLOAD_ARTIFACT, DOWNLOAD_ARTIFACT, DELETE_ARTIFACT, UPDATE_ARTIFACT_METADATA,
            MANAGE_USERS, MANAGE_ROLES,
            MANAGE_RETENTION_POLICIES
        )
        
        /**
         * Permisos asignados al rol de contribuidor de repositorio.
         */
        val CONTRIBUTOR_PERMISSIONS = setOf(
            VIEW_REPOSITORY,
            UPLOAD_ARTIFACT, DOWNLOAD_ARTIFACT, UPDATE_ARTIFACT_METADATA
        )
        
        /**
         * Permisos asignados al rol de lector de repositorio.
         */
        val READER_PERMISSIONS = setOf(
            VIEW_REPOSITORY,
            DOWNLOAD_ARTIFACT
        )
    }
}