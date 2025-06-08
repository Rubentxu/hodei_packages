package dev.rubentxu.hodei.packages.domain.artifactmanagement.handler

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import kotlinx.serialization.json.*
import java.time.Instant
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Implementación del FormatHandler para imágenes Docker (tar)
 * Analiza manifiestos y configuraciones de imágenes Docker utilizando APIs nativas de Kotlin
 */
class DockerFormatHandler : FormatHandler {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?
    ): Result<Pair<ArtifactCoordinates, MetadataSource>> {
        // 1. Intentar extraer del contenido (manifest.json, config.json)
        try {
            val manifestContent = extractManifestJson(content)
            if (manifestContent != null) {
                val manifestObj = json.parseToJsonElement(manifestContent).jsonObject

                // El array RepoTags contiene las etiquetas en formato "nombre:tag"
                val repoTags = manifestObj["RepoTags"]?.jsonArray
                if (repoTags != null && repoTags.isNotEmpty()) {
                    val repoTag = repoTags[0].jsonPrimitive.content
                    val parts = repoTag.split(":")

                    if (parts.size == 2) {
                        // Separar el nombre del repositorio y la imagen
                        val repository = if (parts[0].contains("/")) {
                            parts[0].substringBefore("/")
                        } else {
                            "library" // Repositorio por defecto en Docker Hub
                        }

                        val imageName = if (parts[0].contains("/")) {
                            parts[0].substringAfter("/")
                        } else {
                            parts[0]
                        }

                        val tag = parts[1]

                        val coordinates = ArtifactCoordinates(
                            group = ArtifactGroup(repository),
                            name = imageName,
                            version = ArtifactVersion(tag)
                        )
                        return Result.success(Pair(coordinates, MetadataSource.CONTENT_EXTRACTED))
                    }
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Intentar inferir del nombre del archivo
        // Ejemplos: repo_image-tag.tar, repo-image:tag.tar
        val regex1 = Regex("""([\.\w-]+)[_/]([\.\w-]+)[:-]([\.\w-]+)\.tar""")
        var match = regex1.matchEntire(filename)
        if (match != null) {
            val (repository, imageName, tag) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup(repository),
                name = imageName,
                version = ArtifactVersion(tag)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // Otro formato posible: image-tag.tar
        val regex2 = Regex("""([\.\w-]+)[:-]([\.\w-]+)\.tar""")
        match = regex2.matchEntire(filename)
        if (match != null) {
            val (imageName, tag) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup("library"), // Repositorio por defecto
                name = imageName,
                version = ArtifactVersion(tag)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // 3. Usar metadatos proporcionados
        if (providedMetadata != null) {
            val repository = providedMetadata["repository"] ?: "library"
            val imageName = providedMetadata["name"]
            val tag = providedMetadata["tag"] ?: providedMetadata["version"]

            if (imageName != null && tag != null) {
                val coordinates = ArtifactCoordinates(
                    group = ArtifactGroup(repository),
                    name = imageName,
                    version = ArtifactVersion(tag)
                )
                return Result.success(Pair(coordinates, MetadataSource.USER_PROVIDED))
            }
        }

        return Result.failure(IllegalArgumentException(
            "No se pudieron extraer coordenadas de la imagen Docker: $filename. " +
            "Debe proporcionar explícitamente repository, name y tag."
        ))
    }

    override fun extractMetadataWithSources(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?,
        artifactId: ArtifactId,
        userId: UserId
    ): Result<ArtifactMetadataWithSources> {
        // Valores que se extraerán
        var description: MetadataWithSource<String?>? = null
        var licenses: MetadataWithSource<List<String>?>? = null
        var homepageUrl: MetadataWithSource<String?>? = null
        var repositoryUrl: MetadataWithSource<String?>? = null
        // Corregir tipo: usar Long? en vez de Long para que coincida con el tipo esperado
        var sizeInBytes: MetadataWithSource<Long?>? = MetadataWithSource(content.size.toLong(), MetadataSource.SYSTEM_GENERATED)
        val additionalMetadata = mutableMapOf<String, MetadataWithSource<Any?>>()

        // Calcular checksums y corregir tipo: Map<String, String>? en vez de Map<String, String>
        val checksums = calculateChecksums(content)
        val checksumsWithSource: MetadataWithSource<Map<String, String>?>? = MetadataWithSource(checksums, MetadataSource.SYSTEM_GENERATED)

        // 1. Intentar extraer metadatos del contenido
        try {
            // Extraer manifiesto
            val manifestContent = extractManifestJson(content)
            if (manifestContent != null) {
                val manifestObj = json.parseToJsonElement(manifestContent).jsonObject

                // En Docker, la información de creación está en el ConfigObject
                val configFile = manifestObj["Config"]?.jsonArray?.get(0)?.jsonPrimitive?.content
                if (configFile != null) {
                    val configContent = extractFileFromTar(content, configFile)
                    if (configContent != null) {
                        val configObj = json.parseToJsonElement(configContent).jsonObject

                        // Extraer etiquetas del config (pueden contener metadatos)
                        val labels = configObj["config"]?.jsonObject?.get("Labels")?.jsonObject
                        if (labels != null) {
                            // Descripción
                            val descriptionLabel = labels["org.opencontainers.image.description"]?.jsonPrimitive?.contentOrNull
                                ?: labels["description"]?.jsonPrimitive?.contentOrNull
                            if (descriptionLabel != null && descriptionLabel.isNotEmpty()) {
                                description = MetadataWithSource(descriptionLabel, MetadataSource.CONTENT_EXTRACTED)
                            }

                            // Licencia
                            val licenseLabel = labels["org.opencontainers.image.licenses"]?.jsonPrimitive?.contentOrNull
                                ?: labels["license"]?.jsonPrimitive?.contentOrNull
                            if (licenseLabel != null && licenseLabel.isNotEmpty()) {
                                licenses = MetadataWithSource(
                                    licenseLabel.split(",").map { it.trim() },
                                    MetadataSource.CONTENT_EXTRACTED
                                )
                            }

                            // URL
                            val urlLabel = labels["org.opencontainers.image.url"]?.jsonPrimitive?.contentOrNull
                                ?: labels["url"]?.jsonPrimitive?.contentOrNull
                            if (urlLabel != null && urlLabel.isNotEmpty()) {
                                homepageUrl = MetadataWithSource(urlLabel, MetadataSource.CONTENT_EXTRACTED)
                            }

                            // Repositorio de código fuente
                            val sourceLabel = labels["org.opencontainers.image.source"]?.jsonPrimitive?.contentOrNull
                                ?: labels["vcs-url"]?.jsonPrimitive?.contentOrNull
                            if (sourceLabel != null && sourceLabel.isNotEmpty()) {
                                repositoryUrl = MetadataWithSource(sourceLabel, MetadataSource.CONTENT_EXTRACTED)
                            }

                            // Metadatos adicionales
                            val author = labels["org.opencontainers.image.authors"]?.jsonPrimitive?.contentOrNull
                                ?: labels["maintainer"]?.jsonPrimitive?.contentOrNull
                            if (author != null && author.isNotEmpty()) {
                                additionalMetadata["author"] = MetadataWithSource(author, MetadataSource.CONTENT_EXTRACTED)
                            }

                            val created = labels["org.opencontainers.image.created"]?.jsonPrimitive?.contentOrNull
                            if (created != null && created.isNotEmpty()) {
                                additionalMetadata["created"] = MetadataWithSource(created, MetadataSource.CONTENT_EXTRACTED)
                            }

                            // Versión de software
                            val version = labels["org.opencontainers.image.version"]?.jsonPrimitive?.contentOrNull
                            if (version != null && version.isNotEmpty()) {
                                additionalMetadata["softwareVersion"] = MetadataWithSource(version, MetadataSource.CONTENT_EXTRACTED)
                            }
                        }

                        // Extraer información del sistema operativo y arquitectura
                        val os = configObj["os"]?.jsonPrimitive?.contentOrNull
                        if (os != null && os.isNotEmpty()) {
                            additionalMetadata["os"] = MetadataWithSource(os, MetadataSource.CONTENT_EXTRACTED)
                        }

                        val arch = configObj["architecture"]?.jsonPrimitive?.contentOrNull
                        if (arch != null && arch.isNotEmpty()) {
                            additionalMetadata["architecture"] = MetadataWithSource(arch, MetadataSource.CONTENT_EXTRACTED)
                        }

                        // Extraer información de capas
                        val layers = manifestObj["Layers"]?.jsonArray
                        if (layers != null && layers.isNotEmpty()) {
                            val layersList = layers.map { it.jsonPrimitive.content }
                            additionalMetadata["layers"] = MetadataWithSource(layersList, MetadataSource.CONTENT_EXTRACTED)
                            additionalMetadata["layerCount"] = MetadataWithSource(layers.size, MetadataSource.CONTENT_EXTRACTED)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. No hay metadatos que se puedan inferir confiablemente del nombre del archivo

        // 3. Usar metadatos proporcionados para campos faltantes
        if (providedMetadata != null) {
            if (description == null && providedMetadata["description"] != null) {
                description = MetadataWithSource(providedMetadata["description"], MetadataSource.USER_PROVIDED)
            }

            if (homepageUrl == null && providedMetadata["homepage"] != null) {
                homepageUrl = MetadataWithSource(providedMetadata["homepage"], MetadataSource.USER_PROVIDED)
            }

            if (licenses == null && providedMetadata["license"] != null) {
                val licenseList = providedMetadata["license"]!!.split(",").map { it.trim() }
                licenses = MetadataWithSource(licenseList, MetadataSource.USER_PROVIDED)
            }

            if (repositoryUrl == null && providedMetadata["repository"] != null) {
                repositoryUrl = MetadataWithSource(providedMetadata["repository"], MetadataSource.USER_PROVIDED)
            }

            // Procesar metadatos adicionales proporcionados
            providedMetadata.entries.forEach { (key, value) ->
                if (key !in listOf("description", "homepage", "license", "repository") &&
                    key !in additionalMetadata.keys) {
                    additionalMetadata[key] = MetadataWithSource(value, MetadataSource.USER_PROVIDED)
                }
            }
        }

        // Valores obligatorios
        val createdBy = MetadataWithSource(userId, MetadataSource.SYSTEM_GENERATED)
        val createdAt = MetadataWithSource(Instant.now(), MetadataSource.SYSTEM_GENERATED)

        return Result.success(
            ArtifactMetadataWithSources(
                id = artifactId,
                createdBy = createdBy,
                createdAt = createdAt,
                description = description,
                licenses = licenses,
                homepageUrl = homepageUrl,
                repositoryUrl = repositoryUrl,
                sizeInBytes = sizeInBytes,
                checksums = checksumsWithSource,
                additionalMetadata = additionalMetadata
            )
        )
    }

    override fun determinePackagingType(
        filename: String,
        content: ByteArray
    ): Result<Pair<String, MetadataSource>> {
        // 1. Verificar el contenido
        try {
            val manifestContent = extractManifestJson(content)
            if (manifestContent != null) {
                // Es un archivo tar de Docker
                return Result.success(Pair("docker-image", MetadataSource.CONTENT_EXTRACTED))
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Inferir del nombre de archivo
        if (filename.endsWith(".tar") && (
                filename.contains("docker") ||
                filename.contains("image") ||
                filename.contains(":"))) {
            return Result.success(Pair("docker-image", MetadataSource.FILENAME_INFERRED))
        }

        // 3. Si no podemos determinar, pero se provee como Docker
        return Result.success(Pair("tar", MetadataSource.SYSTEM_GENERATED))
    }

    override fun extractDependencies(content: ByteArray): Result<List<ArtifactDependency>> {
        // En Docker, las dependencias no son explícitas como en otros sistemas de paquetes
        // Podríamos intentar extraer las imágenes base, pero esto requeriría analizar
        // el Dockerfile o los metadatos de la imagen, que no siempre están presentes

        val dependencies = mutableListOf<ArtifactDependency>()

        try {
            val configContent = extractConfigJson(content)
            if (configContent != null) {
                val configObj = json.parseToJsonElement(configContent).jsonObject

                // Intenta obtener la imagen base del historial
                val history = configObj["history"]?.jsonArray
                if (history != null && history.isNotEmpty()) {
                    // El primer elemento del historial suele ser la imagen base
                    val firstLayer = history[0].jsonObject
                    val createdBy = firstLayer["created_by"]?.jsonPrimitive?.contentOrNull ?: ""

                    // Intentar extraer la imagen base de los comandos
                    if (createdBy.contains("FROM ")) {
                        val fromCommand = createdBy.substringAfter("FROM ").trim()
                        val baseImage = fromCommand.split(" ").first()

                        // Analizar baseImage (formato: repo/image:tag)
                        val baseImageParts = baseImage.split(":")
                        val imageName = baseImageParts[0]
                        val tag = if (baseImageParts.size > 1) baseImageParts[1] else "latest"

                        // Separar repo y nombre si existe
                        val (group, name) = if (imageName.contains("/")) {
                            Pair(
                                ArtifactGroup(imageName.substringBefore("/")),
                                imageName.substringAfter("/")
                            )
                        } else {
                            Pair(ArtifactGroup("library"), imageName)
                        }

                        dependencies.add(
                            ArtifactDependency(
                                coordinates = ArtifactCoordinates(
                                    group = group,
                                    name = name,
                                    version = ArtifactVersion(tag)
                                ),
                                scope = "base-image",
                                isOptional = false,
                                versionConstraint = tag
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Si hay errores, retornamos lista vacía
        }

        return Result.success(dependencies)
    }

    override fun generateDescriptor(artifact: Artifact): Result<String> {
        val coordinates = artifact.coordinates
        val meta = artifact.metadata

        val jsonObject = buildJsonObject {
            // Crear el formato de RepoTags
            val repoTag = "${coordinates.group.value}/${coordinates.name}:${coordinates.version.value}"
            putJsonArray("RepoTags") {
                add(JsonPrimitive(repoTag))
            }

            // Información de configuración
            putJsonObject("config") {
                // Labels
                putJsonObject("Labels") {
                    meta.description?.let {
                        put("org.opencontainers.image.description", it)
                    }
                    meta.licenses?.let {
                        put("org.opencontainers.image.licenses", it.joinToString(","))
                    }
                    meta.homepageUrl?.let {
                        put("org.opencontainers.image.url", it)
                    }
                    meta.repositoryUrl?.let {
                        put("org.opencontainers.image.source", it)
                    }
                }

                // Metadatos extras
                put("created", Instant.now().toString())
                put("architecture", "amd64") // Por defecto
                put("os", "linux")   // Por defecto
            }

            // Digest (simulado)
            val digest = "sha256:" + meta.checksums?.get("SHA-256") ?: "0000000000000000000000000000000000000000000000000000000000000000"
            put("digest", digest)
        }

        return Result.success(json.encodeToString(JsonObject.serializer(), jsonObject))
    }

    override fun validateMetadataConsistency(
        extracted: ArtifactMetadataWithSources,
        provided: Map<String, String>
    ): Result<Boolean> {
        val inconsistencies = mutableListOf<String>()

        // Las imágenes Docker tienen menos restricciones de consistencia
        // que otros formatos. Verificamos solo algunos campos críticos.

        extracted.description?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["description"] != null &&
                it.value != provided["description"]) {
                inconsistencies.add("La descripción proporcionada ('${provided["description"]}') " +
                    "no coincide con las etiquetas de la imagen ('${it.value}')")
            }
        }

        return if (inconsistencies.isEmpty()) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException(
                "Inconsistencias encontradas entre metadatos proporcionados y extraídos: " +
                    "${inconsistencies.joinToString("; ")}"
            ))
        }
    }

    // ===== Métodos de utilidad =====

    /**
     * Extrae el manifest.json de un archivo tar de Docker
     */
    private fun extractManifestJson(content: ByteArray): String? {
        try {
            return extractFileFromTar(content, "manifest.json")
        } catch (e: Exception) {
            // Si hay error, retornamos null
        }
        return null
    }

    /**
     * Extrae el config.json de un archivo tar de Docker
     */
    private fun extractConfigJson(content: ByteArray): String? {
        try {
            val manifestContent = extractManifestJson(content)
            if (manifestContent != null) {
                val manifestObj = json.parseToJsonElement(manifestContent).jsonObject

                // El array Config contiene la ruta al archivo de configuración
                val configFile = manifestObj["Config"]?.jsonArray?.get(0)?.jsonPrimitive?.content
                if (configFile != null) {
                    return extractFileFromTar(content, configFile)
                }
            }
        } catch (e: Exception) {
            // Si hay error, retornamos null
        }
        return null
    }

    /**
     * Extrae un archivo específico de un tar
     */
    private fun extractFileFromTar(content: ByteArray, fileName: String): String? {
        ByteArrayInputStream(content).use { inputStream ->
            return extractFileFromTarStream(inputStream) { entryName ->
                entryName.endsWith(fileName)
            }
        }
    }

    /**
     * Extrae un archivo de un stream tar basado en un predicado para el nombre de la entrada
     */
    private fun extractFileFromTarStream(
        inputStream: InputStream,
        predicate: (String) -> Boolean
    ): String? {
        val buffer = ByteArray(512)  // Tamaño del bloque en archivos tar

        while (true) {
            // Leer encabezado
            val bytesRead = inputStream.readNBytes(buffer, 0, 512)
            if (bytesRead < 512) return null // EOF o archivo inválido

            // Comprobar si es bloque final (solo ceros)
            if (buffer.all { it.toInt() == 0 }) return null

            // Extraer nombre del archivo (primeros 100 bytes)
            val nameBytes = buffer.copyOfRange(0, 100)
            val nullPos = nameBytes.indexOfFirst { it.toInt() == 0 }
            val entryName = String(
                nameBytes,
                0,
                if (nullPos >= 0) nullPos else nameBytes.size,
                Charsets.UTF_8
            )

            // Extraer tamaño del archivo (offset 124, 12 bytes, octal)
            val sizeBytes = buffer.copyOfRange(124, 136)
            val nullPosSizeBytes = sizeBytes.indexOfFirst { it.toInt() == 0 }
            val sizeStr = String(
                sizeBytes,
                0,
                if (nullPosSizeBytes >= 0) nullPosSizeBytes else sizeBytes.size
            )
            val fileSize = sizeStr.trim().toLongOrNull(8) ?: 0L

            if (predicate(entryName)) {
                // Es el archivo que buscamos, extraer su contenido
                val contentBuffer = ByteArrayOutputStream()
                var remaining = fileSize
                val readBuffer = ByteArray(8192)

                while (remaining > 0) {
                    val chunkSize = minOf(remaining, readBuffer.size.toLong()).toInt()
                    val readCount = inputStream.readNBytes(readBuffer, 0, chunkSize)
                    if (readCount <= 0) break

                    contentBuffer.write(readBuffer, 0, readCount)
                    remaining -= readCount
                }

                return contentBuffer.toString(Charsets.UTF_8.name())
            } else {
                // No es el archivo que buscamos, saltarlo
                val blocksToSkip = (fileSize + 511) / 512 // Redondear hacia arriba
                inputStream.skip(blocksToSkip * 512)
            }
        }
    }

    /**
     * Calcula los checksums del contenido del artefacto
     */
    private fun calculateChecksums(content: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Calcular SHA-256 (usado por Docker)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(content)
        result["SHA-256"] = sha256.joinToString("") { "%02x".format(it) }

        // Calcular SHA-512 (opcional para mayor seguridad)
        val sha512 = MessageDigest.getInstance("SHA-512").digest(content)
        result["SHA-512"] = sha512.joinToString("") { "%02x".format(it) }

        return result
    }
}
