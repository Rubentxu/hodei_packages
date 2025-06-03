package dev.rubentxu.hodei.packages.domain.model.repository

/**
 * Tipos de repositorios soportados por el sistema.
 */
enum class RepositoryType {
    /**
     * Repositorio Maven para artefactos Java/Kotlin
     */
    MAVEN,
    
    /**
     * Repositorio NPM para paquetes JavaScript/Node.js
     */
    NPM;
    
    companion object {
        /**
         * Obtiene el tipo de repositorio a partir de su nombre.
         * @param name Nombre del tipo de repositorio
         * @return RepositoryType correspondiente o null si no existe
         */
        fun fromString(name: String): RepositoryType? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
    }
}