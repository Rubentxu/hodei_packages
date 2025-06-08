package dev.rubentxu.hodei.packages.domain.artifactmanagement.handler

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import kotlinx.serialization.json.*
import java.time.Instant
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Implementación del FormatHandler para paquetes NPM (tgz)
 * Descomprime y extrae metadatos de package.json directamente utilizando APIs nativas de Kotlin
 */
class NpmFormatHandler : FormatHandler {

    private val json = Json { ignoreUnknownKeys = true }

    override fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?
    ): Result<Pair<ArtifactCoordinates, MetadataSource>> {
        // 1. Intentar extraer del contenido (package.json)
        try {
            val packageJsonContent = extractPackageJson(content)
            if (packageJsonContent != null) {
                val jsonObj = json.parseToJsonElement(packageJsonContent).jsonObject

                val name = jsonObj["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val version = jsonObj["version"]?.jsonPrimitive?.contentOrNull ?: ""

                if (name.isNotEmpty() && version.isNotEmpty()) {
                    // Parsear el nombre para detectar si tiene scope (@org/package)
                    val group = if (name.startsWith("@")) {
                        ArtifactGroup(name.substring(0, name.indexOf('/')))
                    } else {
                        ArtifactGroup.NONE
                    }

                    // El nombre sin el scope
                    val baseName = if (name.contains("/")) {
                        name.substring(name.indexOf('/') + 1)
                    } else {
                        name
                    }

                    val coordinates = ArtifactCoordinates(
                        group = group,
                        name = baseName,
                        version = ArtifactVersion(version)
                    )
                    return Result.success(Pair(coordinates, MetadataSource.CONTENT_EXTRACTED))
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Intentar inferir del nombre del archivo
        val scopeRegex = Regex("""(@[\w-]+/)?([\w-]+)-(\d+\.\d+\.\d+(?:-[\w.-]+)?)\.tgz""")
        val match = scopeRegex.matchEntire(filename)
        if (match != null) {
            val (scope, name, version) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = if (scope.isNotBlank()) ArtifactGroup(scope.removeSuffix("/")) else ArtifactGroup.NONE,
                name = name,
                version = ArtifactVersion(version)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // 3. Si se proporcionan metadatos, usarlos
        if (providedMetadata != null) {
            val name = providedMetadata["name"]
            val version = providedMetadata["version"]
            val scope = providedMetadata["scope"]

            if (name != null && version != null) {
                val group = scope?.let { ArtifactGroup(it) } ?: ArtifactGroup.NONE
                val coordinates = ArtifactCoordinates(
                    group = group,
                    name = name,
                    version = ArtifactVersion(version)
                )
                return Result.success(Pair(coordinates, MetadataSource.USER_PROVIDED))
            }
        }

        return Result.failure(IllegalArgumentException(
            "No se pudieron extraer coordenadas del paquete NPM: $filename. " +
            "Debe proporcionar explícitamente name y version."
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
        // Corregir tipo: usar Long? para que coincida con el tipo esperado
        var sizeInBytes: MetadataWithSource<Long?>? = MetadataWithSource(content.size.toLong(), MetadataSource.SYSTEM_GENERATED)
        val additionalMetadata = mutableMapOf<String, MetadataWithSource<Any?>>()

        // Calcular checksums y corregir tipo: Map<String, String>? para que coincida con el tipo esperado
        val checksums = calculateChecksums(content)
        val checksumsWithSource: MetadataWithSource<Map<String, String>?>? = MetadataWithSource(checksums, MetadataSource.SYSTEM_GENERATED)

        // 1. Intentar extraer metadatos del package.json
        try {
            val packageJsonContent = extractPackageJson(content)
            if (packageJsonContent != null) {
                val jsonObj = json.parseToJsonElement(packageJsonContent).jsonObject

                // Extraer metadatos básicos
                jsonObj["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                    description = MetadataWithSource(it, MetadataSource.CONTENT_EXTRACTED)
                }

                jsonObj["homepage"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                    homepageUrl = MetadataWithSource(it, MetadataSource.CONTENT_EXTRACTED)
                }

                // Licencias (pueden ser string o array)
                val licensesValue = when {
                    jsonObj["license"]?.jsonPrimitive != null -> {
                        listOf(jsonObj["license"]!!.jsonPrimitive.content)
                    }
                    jsonObj["licenses"]?.jsonArray != null -> {
                        jsonObj["licenses"]!!.jsonArray.mapNotNull {
                            if (it is JsonObject) it["type"]?.jsonPrimitive?.content else null
                        }
                    }
                    else -> null
                }

                if (!licensesValue.isNullOrEmpty()) {
                    licenses = MetadataWithSource(licensesValue, MetadataSource.CONTENT_EXTRACTED)
                }

                // URL del repositorio
                val repository = jsonObj["repository"]
                when {
                    repository is JsonObject -> {
                        repository.jsonObject["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                            repositoryUrl = MetadataWithSource(it, MetadataSource.CONTENT_EXTRACTED)
                        }
                    }
                    repository is JsonPrimitive && !repository.content.isNullOrBlank() -> {
                        repositoryUrl = MetadataWithSource(repository.content, MetadataSource.CONTENT_EXTRACTED)
                    }
                }

                // Metadatos adicionales
                val author = jsonObj["author"]
                when {
                    author is JsonObject -> {
                        author.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let {
                            additionalMetadata["author"] = MetadataWithSource(it, MetadataSource.CONTENT_EXTRACTED)
                        }
                    }
                    author is JsonPrimitive -> {
                        additionalMetadata["author"] = MetadataWithSource(author.content, MetadataSource.CONTENT_EXTRACTED)
                    }
                }

                // Keywords como tags
                val keywords = jsonObj["keywords"]?.jsonArray
                if (keywords != null && keywords.isNotEmpty()) {
                    val keywordsList = keywords.mapNotNull { it.jsonPrimitive.contentOrNull }
                    additionalMetadata["keywords"] = MetadataWithSource(keywordsList, MetadataSource.CONTENT_EXTRACTED)
                }

                // Engine requirements
                jsonObj["engines"]?.jsonObject?.let { engines ->
                    engines["node"]?.jsonPrimitive?.contentOrNull?.let { nodeVersion ->
                        additionalMetadata["nodeVersion"] = MetadataWithSource(nodeVersion, MetadataSource.CONTENT_EXTRACTED)
                    }
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. No hay metadatos que se puedan inferir confiablemente del nombre de archivo

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
        // Para NPM, el formato de empaquetado siempre es tgz
        // 1. Verificar el contenido (magic bytes de gzip)
        if (content.size >= 2 && content[0] == 0x1F.toByte() && content[1] == 0x8B.toByte()) {
            try {
                // Intentar descomprimir para verificar que es realmente un tgz válido
                GZIPInputStream(ByteArrayInputStream(content)).use { _ ->
                    return Result.success(Pair("tgz", MetadataSource.CONTENT_EXTRACTED))
                }
            } catch (e: Exception) {
                // Continuar con la siguiente estrategia
            }
        }

        // 2. Inferir del nombre de archivo
        if (filename.endsWith(".tgz") || filename.endsWith(".tar.gz")) {
            return Result.success(Pair("tgz", MetadataSource.FILENAME_INFERRED))
        }

        // 3. Si no podemos determinar, pero sabemos que es un paquete NPM
        return Result.success(Pair("tgz", MetadataSource.SYSTEM_GENERATED))
    }

    override fun extractDependencies(content: ByteArray): Result<List<ArtifactDependency>> {
        val dependencies = mutableListOf<ArtifactDependency>()

        try {
            val packageJsonContent = extractPackageJson(content)
            if (packageJsonContent != null) {
                val jsonObj = json.parseToJsonElement(packageJsonContent).jsonObject

                // Extraer dependencias normales
                jsonObj["dependencies"]?.jsonObject?.let { deps ->
                    dependencies.addAll(extractDependenciesFromJsonObject(deps, "runtime"))
                }

                // Extraer dependencias de desarrollo
                jsonObj["devDependencies"]?.jsonObject?.let { devDeps ->
                    dependencies.addAll(extractDependenciesFromJsonObject(devDeps, "dev"))
                }

                // Extraer peer dependencies
                jsonObj["peerDependencies"]?.jsonObject?.let { peerDeps ->
                    dependencies.addAll(extractDependenciesFromJsonObject(peerDeps, "peer"))
                }

                // Extraer optional dependencies
                jsonObj["optionalDependencies"]?.jsonObject?.let { optionalDeps ->
                    dependencies.addAll(extractDependenciesFromJsonObject(optionalDeps, "optional", true))
                }

                return Result.success(dependencies)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.success(emptyList())
    }

    override fun generateDescriptor(artifact: Artifact): Result<String> {
        val coordinates = artifact.coordinates
        val meta = artifact.metadata

        val jsonObject = buildJsonObject {
            // Nombre con scope si está presente
            val name = if (coordinates.group != ArtifactGroup.NONE) {
                "${coordinates.group.value}/${coordinates.name}"
            } else {
                coordinates.name
            }

            put("name", name)
            put("version", coordinates.version.value)

            meta.description?.let { put("description", it) }
            meta.homepageUrl?.let { put("homepage", it) }

            // Licencias
            meta.licenses?.let {
                if (it.size == 1) {
                    put("license", it.first())
                } else if (it.isNotEmpty()) {
                    putJsonArray("licenses") {
                        it.forEach { license ->
                            addJsonObject {
                                put("type", license)
                            }
                        }
                    }
                }
            }

            // URL de repositorio
            meta.repositoryUrl?.let {
                putJsonObject("repository") {
                    put("type", "git") // Asumimos Git por defecto
                    put("url", it)
                }
            }

            // Dependencias
            artifact.dependencies?.takeIf { it.isNotEmpty() }?.let { deps ->
                val dependenciesByScope = deps.groupBy { dep ->
                    when {
                        dep.isOptional -> "optionalDependencies"
                        dep.scope == "dev" -> "devDependencies"
                        dep.scope == "peer" -> "peerDependencies"
                        else -> "dependencies"
                    }
                }

                dependenciesByScope.forEach { (scope, scopedDeps) ->
                    putJsonObject(scope) {
                        scopedDeps.forEach { dep ->
                            val depVersion = dep.versionConstraint ?: dep.coordinates.version.value
                            put(dep.coordinates.name, depVersion)
                        }
                    }
                }
            }
        }

        return Result.success(json.encodeToString(JsonObject.serializer(), jsonObject))
    }

    override fun validateMetadataConsistency(
        extracted: ArtifactMetadataWithSources,
        provided: Map<String, String>
    ): Result<Boolean> {
        val inconsistencies = mutableListOf<String>()

        // Verificar consistencia de metadatos críticos
        extracted.description?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["description"] != null &&
                it.value != provided["description"]) {
                inconsistencies.add("La descripción proporcionada ('${provided["description"]}') no coincide con la del package.json ('${it.value}')")
            }
        }

        extracted.homepageUrl?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["homepage"] != null &&
                it.value != provided["homepage"]) {
                inconsistencies.add("La homepage proporcionada ('${provided["homepage"]}') no coincide con la del package.json ('${it.value}')")
            }
        }

        // Para licencias, permitimos más flexibilidad debido a diferentes formatos

        return if (inconsistencies.isEmpty()) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException(
                "Inconsistencias encontradas entre metadatos proporcionados y extraídos: ${inconsistencies.joinToString("; ")}"
            ))
        }
    }

    // ===== Métodos de utilidad =====

    /**
     * Extrae el package.json de un archivo tgz de NPM usando APIs nativas de Java
     */
    private fun extractPackageJson(content: ByteArray): String? {
        try {
            GZIPInputStream(ByteArrayInputStream(content)).use { gzipStream ->
                return extractFileFromTarStream(gzipStream) { entryName ->
                    // El package.json está en la raíz o en "package/package.json"
                    entryName == "package.json" || entryName == "package/package.json"
                }
            }
        } catch (e: Exception) {
            // Si ocurre un error, retornamos null
        }
        return null
    }

    /**
     * Extrae un archivo de un stream tar basado en un predicado para el nombre de la entrada
     *
     * @param inputStream El stream del contenido del archivo tar
     * @param predicate Función que determina qué archivo extraer basado en su nombre
     * @return Contenido del archivo como string o null si no se encuentra
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
     * Extrae dependencias de un objeto JSON de package.json
     */
    private fun extractDependenciesFromJsonObject(
        jsonObj: JsonObject,
        scope: String,
        optional: Boolean = false
    ): List<ArtifactDependency> {
        val dependencies = mutableListOf<ArtifactDependency>()

        jsonObj.entries.forEach { (depName, versionElement) ->
            val versionConstraint = versionElement.jsonPrimitive.content

            // Para NPM, determinar si hay un scope (@org/package)
            val (group, name) = if (depName.startsWith("@")) {
                val scopeEndIndex = depName.indexOf('/')
                if (scopeEndIndex > 0) {
                    Pair(
                        ArtifactGroup(depName.substring(0, scopeEndIndex)),
                        depName.substring(scopeEndIndex + 1)
                    )
                } else {
                    Pair(ArtifactGroup.NONE, depName)
                }
            } else {
                Pair(ArtifactGroup.NONE, depName)
            }

            dependencies.add(
                ArtifactDependency(
                    coordinates = ArtifactCoordinates(
                        group = group,
                        name = name,
                        // Para la versión, usamos una versión genérica ya que el constraint es lo importante
                        version = ArtifactVersion("0.0.0")
                    ),
                    scope = scope,
                    isOptional = optional,
                    versionConstraint = versionConstraint
                )
            )
        }

        return dependencies
    }

    /**
     * Calcula los checksums del contenido del artefacto
     */
    private fun calculateChecksums(content: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Calcular SHA-1
        val sha1 = MessageDigest.getInstance("SHA-1").digest(content)
        result["SHA-1"] = sha1.joinToString("") { "%02x".format(it) }

        // Calcular SHA-256
        val sha256 = MessageDigest.getInstance("SHA-256").digest(content)
        result["SHA-256"] = sha256.joinToString("") { "%02x".format(it) }

        // Calcular MD5 (usado por npm)
        val md5 = MessageDigest.getInstance("MD5").digest(content)
        result["MD5"] = md5.joinToString("") { "%02x".format(it) }

        return result
    }
}
