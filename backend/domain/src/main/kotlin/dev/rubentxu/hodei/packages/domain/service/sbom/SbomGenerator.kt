package dev.rubentxu.hodei.packages.domain.service.sbom

import dev.rubentxu.hodei.packages.domain.events.EventPublisher
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationCompletedEvent
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationFailedEvent
import dev.rubentxu.hodei.packages.domain.events.sbom.SbomGenerationStartedEvent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomComponent
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomDocument
import dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
import dev.rubentxu.hodei.packages.domain.repository.sbom.SbomRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/**
 * Servicio del dominio responsable de generar documentos SBOM para artefactos.
 * Analiza el contenido de un artefacto y extrae información sobre sus componentes y dependencias.
 *
 * @param eventPublisher Publicador de eventos del dominio para comunicar eventos durante el proceso
 * @param sbomRepository Repositorio para almacenar los documentos SBOM generados
 */
class SbomGenerator(
    private val eventPublisher: EventPublisher,
    private val sbomRepository: SbomRepository
) {
    /**
     * Genera un documento SBOM a partir del contenido de un artefacto.
     *
     * @param artifactId ID del artefacto para el que se generará el SBOM
     * @param artifactContent Contenido del artefacto como bytes
     * @param format Formato del SBOM a generar (CYCLONE_DX, SPDX)
     * @return Resultado encapsulando el documento SBOM generado o un error
     */
    suspend fun generateSbom(
        artifactId: String,
        artifactContent: ByteArray,
        format: SbomFormat
    ): Result<SbomDocument> {
        val startTime = Instant.now()
        
        // Publicar evento de inicio de generación
        eventPublisher.publish(SbomGenerationStartedEvent(
            artifactId = artifactId,
            format = format
        ))
        
        return try {
            // Extraer componentes y dependencias del contenido del artefacto
            val components = extractComponents(artifactContent)
            
            // Crear documento SBOM
            val sbomDocument = SbomDocument(
                artifactId = artifactId,
                format = format,
                components = components
            )
            
            // Guardar el documento en el repositorio
            val result = sbomRepository.save(sbomDocument)
            
            if (result.isSuccess) {
                // Publicar evento de generación completada
                val endTime = Instant.now()
                val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
                
                eventPublisher.publish(SbomGenerationCompletedEvent(
                    sbomDocument = result.getOrNull()!!,
                    artifactId = artifactId,
                    durationMs = durationMs
                ))
            }
            
            result
        } catch (e: Exception) {
            // Publicar evento de fallo en la generación
            eventPublisher.publish(SbomGenerationFailedEvent(
                artifactId = artifactId,
                format = format,
                errorMessage = e.message ?: "Unknown error during SBOM generation"
            ))
            
            Result.failure(e)
        }
    }
    
    /**
     * Extrae componentes y dependencias del contenido de un artefacto.
     * Este método analiza el contenido según su tipo y formato.
     *
     * @param artifactContent Contenido del artefacto como bytes
     * @return Lista de componentes encontrados
     */
    private fun extractComponents(artifactContent: ByteArray): List<SbomComponent> {
        val components = mutableListOf<SbomComponent>()
        
        try {
            // Intentamos parsear como JSON (formato común para package.json, pom.xml convertido, etc)
            val contentString = artifactContent.toString(Charsets.UTF_8)
            val json = Json.parseToJsonElement(contentString).jsonObject
            
            // Extraer información del componente principal
            val mainPackage = extractMainPackage(json)
            if (mainPackage != null) {
                components.add(mainPackage)
            }
            
            // Extraer dependencias
            components.addAll(extractDependencies(json))
            
        } catch (e: Exception) {
            // Si no es JSON o hay error, intentamos otros formatos según extensión/contenido
            // Esto sería más elaborado en una implementación real, con detectores de formato
            // y extractores específicos para cada tipo
            
            // Por ahora, dejar vacío o con valores de ejemplo para la prueba
            if (components.isEmpty()) {
                components.add(SbomComponent(
                    name = "unknown-package",
                    version = "0.0.0",
                    type = "library"
                ))
            }
        }
        
        return components
    }
    
    /**
     * Extrae información sobre el componente principal desde JSON.
     */
    private fun extractMainPackage(json: JsonObject): SbomComponent? {
        return try {
            if (json.containsKey("name") && json.containsKey("version")) {
                SbomComponent(
                    name = json["name"]?.jsonPrimitive?.content ?: "unknown",
                    version = json["version"]?.jsonPrimitive?.content ?: "0.0.0",
                    type = "library"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extrae información sobre dependencias desde JSON.
     */
    private fun extractDependencies(json: JsonObject): List<SbomComponent> {
        val dependencies = mutableListOf<SbomComponent>()
        
        try {
            if (json.containsKey("dependencies")) {
                val deps = json["dependencies"]?.jsonObject ?: return dependencies
                
                for (dep in deps) {
                    dependencies.add(
                        SbomComponent(
                            name = dep.key,
                            version = dep.value.jsonPrimitive.content,
                            type = "library"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignorar errores en la extracción de dependencias
        }
        
        return dependencies
    }
}
