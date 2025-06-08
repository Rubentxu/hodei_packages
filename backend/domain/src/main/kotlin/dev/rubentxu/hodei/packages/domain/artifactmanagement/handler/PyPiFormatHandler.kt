package dev.rubentxu.hodei.packages.domain.artifactmanagement.handler

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.time.Instant
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Implementación del FormatHandler para paquetes Python (wheel, egg, sdist)
 * Descomprime y extrae metadatos de los archivos METADATA o PKG-INFO
 */
class PyPiFormatHandler : FormatHandler {

    override fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?
    ): Result<Pair<ArtifactCoordinates, MetadataSource>> {
        // 1. Intentar extraer del contenido (METADATA o PKG-INFO)
        try {
            val metadata = extractPackageMetadata(content, filename)
            if (metadata != null) {
                // En Python, el nombre y la versión están en campos específicos
                val name = metadata["Name"]?.trim()
                val version = metadata["Version"]?.trim()

                if (name != null && version != null) {
                    val coordinates = ArtifactCoordinates(
                        group = ArtifactGroup.NONE, // Python no tiene concepto de grupo
                        name = name,
                        version = ArtifactVersion(version)
                    )
                    return Result.success(Pair(coordinates, MetadataSource.CONTENT_EXTRACTED))
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Intentar inferir del nombre del archivo

        // Para wheels (formato moderno)
        val wheelRegex = Regex("""([\w.-]+)-([\d.]+)(?:-.*)?-.*\.whl""")
        var match = wheelRegex.matchEntire(filename)
        if (match != null) {
            val (name, version) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup.NONE,
                name = name,
                version = ArtifactVersion(version)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // Para eggs (formato legacy)
        val eggRegex = Regex("""([\w.-]+)-([\d.]+)(?:-py[\d.]+)?\.egg""")
        match = eggRegex.matchEntire(filename)
        if (match != null) {
            val (name, version) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup.NONE,
                name = name,
                version = ArtifactVersion(version)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // Para sdist (tar.gz o zip)
        val sdistRegex = Regex("""([\w.-]+)-([\d.]+)\.(?:tar\.gz|zip)""")
        match = sdistRegex.matchEntire(filename)
        if (match != null) {
            val (name, version) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup.NONE,
                name = name,
                version = ArtifactVersion(version)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // 3. Último recurso: usar metadatos proporcionados
        if (providedMetadata != null) {
            val name = providedMetadata["name"]
            val version = providedMetadata["version"]

            if (name != null && version != null) {
                val coordinates = ArtifactCoordinates(
                    group = ArtifactGroup.NONE, // Python no usa grupos
                    name = name,
                    version = ArtifactVersion(version)
                )
                return Result.success(Pair(coordinates, MetadataSource.USER_PROVIDED))
            }
        }

        return Result.failure(IllegalArgumentException(
            "No se pudieron extraer coordenadas del paquete Python: $filename. " +
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
        // Corregir tipo: usar Long? en vez de Long para que coincida con el tipo esperado
        val sizeInBytes: MetadataWithSource<Long?>? = MetadataWithSource(content.size.toLong(), MetadataSource.SYSTEM_GENERATED) // Changed to val
        val additionalMetadata = mutableMapOf<String, MetadataWithSource<Any?>>()

        // Calcular checksums y corregir tipo: Map<String, String>? en vez de Map<String, String>
        val checksums = calculateChecksums(content)
        val checksumsWithSource: MetadataWithSource<Map<String, String>?>? = MetadataWithSource(checksums, MetadataSource.SYSTEM_GENERATED)

        // 1. Intentar extraer metadatos del archivo de metadatos
        try {
            val metadata = extractPackageMetadata(content, filename)
            if (metadata != null) {
                // Descripción puede estar en Summary o Description
                val summary = metadata["Summary"]
                val fullDescription = metadata["Description"]

                if (!summary.isNullOrBlank()) {
                    description = MetadataWithSource(summary.trim(), MetadataSource.CONTENT_EXTRACTED)
                } else if (!fullDescription.isNullOrBlank()) {
                    // Si hay una descripción larga, usamos solo la primera línea como resumen
                    description = MetadataWithSource(
                        fullDescription.lineSequence().first().trim(),
                        MetadataSource.CONTENT_EXTRACTED
                    )
                }

                // Licencia
                val license = metadata["License"]
                if (!license.isNullOrBlank()) {
                    licenses = MetadataWithSource(listOf(license.trim()), MetadataSource.CONTENT_EXTRACTED)
                }

                // URL de la página principal
                val homepage = metadata["Home-page"] ?: metadata["Project-URL"]
                if (!homepage.isNullOrBlank()) {
                    homepageUrl = MetadataWithSource(homepage.trim(), MetadataSource.CONTENT_EXTRACTED)
                }

                // Datos del autor
                val author = metadata["Author"]
                val authorEmail = metadata["Author-email"]
                if (!author.isNullOrBlank()) {
                    val authorInfo = if (!authorEmail.isNullOrBlank()) {
                        "$author <$authorEmail>"
                    } else {
                        author
                    }
                    additionalMetadata["author"] = MetadataWithSource(authorInfo.trim(), MetadataSource.CONTENT_EXTRACTED)
                }

                // Keywords/Tags
                val keywords = metadata["Keywords"]
                if (!keywords.isNullOrBlank()) {
                    val keywordsList = keywords.split(",").map { it.trim() }
                    additionalMetadata["keywords"] = MetadataWithSource(keywordsList, MetadataSource.CONTENT_EXTRACTED)
                }

                // Clasificadores (importante para PyPI)
                val classifiers = extractClassifiers(metadata)
                if (classifiers.isNotEmpty()) {
                    additionalMetadata["classifiers"] = MetadataWithSource(classifiers, MetadataSource.CONTENT_EXTRACTED)

                    // Extraer URLs de repositorio de los clasificadores si es posible
                    val repoClassifier = classifiers.find { it.contains("Repository") }
                    if (repoClassifier != null) {
                        val repoUrl = repoClassifier.substringAfter("Repository").trim()
                        repositoryUrl = MetadataWithSource(repoUrl, MetadataSource.CONTENT_EXTRACTED)
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
        // 1. Verificar el contenido (no hay una forma confiable de determinar el tipo solo por el contenido)

        // 2. Inferir del nombre de archivo (más confiable en Python)
        if (filename.endsWith(".whl")) {
            return Result.success(Pair("whl", MetadataSource.FILENAME_INFERRED))
        }

        if (filename.endsWith(".egg")) {
            return Result.success(Pair("egg", MetadataSource.FILENAME_INFERRED))
        }

        if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            return Result.success(Pair("sdist", MetadataSource.FILENAME_INFERRED))
        }

        if (filename.endsWith(".zip")) {
            return Result.success(Pair("sdist-zip", MetadataSource.FILENAME_INFERRED))
        }

        // 3. Si no podemos determinar, usamos un valor genérico
        return Result.success(Pair("python-package", MetadataSource.SYSTEM_GENERATED))
    }

    override fun extractDependencies(content: ByteArray): Result<List<ArtifactDependency>> {
        val dependencies = mutableListOf<ArtifactDependency>()

        try {
            val metadata = extractPackageMetadata(content)
            if (metadata != null) {
                // En Python, las dependencias pueden estar en varios formatos
                // Buscamos campos "Requires-Dist", "Requires" o "Requires-Python"

                // Requires-Dist (formato moderno, PEP 566)
                val requiresDist = metadata.filterKeys { it.startsWith("Requires-Dist") }
                for ((_, value) in requiresDist) {
                    // Formato: "package (>=1.0); extra == 'dev'"
                    val requirement = parseRequiresDist(value)
                    if (requirement != null) {
                        dependencies.add(requirement)
                    }
                }

                // Requires (formato antiguo)
                val requires = metadata["Requires"]
                if (!requires.isNullOrBlank()) {
                    requires.split(",").forEach { req ->
                        val requirement = parseRequiresLegacy(req.trim())
                        if (requirement != null) {
                            dependencies.add(requirement)
                        }
                    }
                }

                // Requires-Python (específico para la versión de Python)
                val requiresPython = metadata["Requires-Python"]
                if (!requiresPython.isNullOrBlank()) {
                    dependencies.add(
                        ArtifactDependency(
                            coordinates = ArtifactCoordinates(
                                group = ArtifactGroup.NONE,
                                name = "python",
                                version = ArtifactVersion("0.0.0") // Placeholder
                            ),
                            scope = "environment",
                            isOptional = false,
                            versionConstraint = requiresPython.trim()
                        )
                    )
                }

                return Result.success(dependencies)
            }
        } catch (e: Exception) { 
            return Result.failure(e)
        }

        return Result.success(emptyList())
    }

    override fun generateDescriptor(artifact: Artifact): Result<String> {
        val meta = artifact.metadata

        // Crear un descriptor en formato METADATA (PEP 566)
        val metadataLines = mutableListOf<String>()

        // Campos obligatorios
        metadataLines.add("Metadata-Version: 2.1")
        metadataLines.add("Name: ${artifact.coordinates.name}")
        metadataLines.add("Version: ${artifact.coordinates.version.value}")

        // Campos opcionales
        meta.description?.let { metadataLines.add("Summary: $it") }
        meta.homepageUrl?.let { metadataLines.add("Home-page: $it") }
        meta.licenses?.firstOrNull()?.let { metadataLines.add("License: $it") }

        // Dependencias
        artifact.dependencies?.forEach {  dep: ArtifactDependency ->
            val requirement = buildString {
                append(dep.coordinates.name)
                // dep.coordinates.version es ArtifactVersion y no es nullable.
                // La restricción de versión es dep.versionConstraint
                if (dep.versionConstraint != null) {
                    append(" (${dep.versionConstraint})")
                } else {
                    // Si no hay versionConstraint, podríamos querer añadir la versión fija de las coordenadas
                    // o nada si la política es que versionConstraint es el único que se usa aquí.
                    // Por ahora, si no hay constraint, no se añade nada de versión.
                }
                if (dep.scope != null && dep.scope != "runtime") {
                    append("; extra == '${dep.scope}'")
                }
            }
            metadataLines.add("Requires-Dist: $requirement")
        }

        return Result.success(metadataLines.joinToString("\n"))
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
                inconsistencies.add("La descripción proporcionada ('${provided["description"]}') no coincide con la extraída del paquete ('${it.value}')")
            }
        }

        extracted.homepageUrl?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["homepage"] != null &&
                it.value != provided["homepage"]) {
                inconsistencies.add("La homepage proporcionada ('${provided["homepage"]}') no coincide con la extraída del paquete ('${it.value}')")
            }
        }

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
     * Extrae los metadatos de un paquete Python, buscando en los formatos comunes
     */
    private fun extractPackageMetadata(
        content: ByteArray,
        filename: String = ""
    ): Map<String, String>? {
        // Detectar el formato según la extensión
        return when {
            filename.endsWith(".whl") -> extractWheelMetadata(content)
            filename.endsWith(".egg") -> extractEggMetadata(content)
            filename.endsWith(".tar.gz") || filename.endsWith(".tgz") -> extractSdistMetadata(content)
            filename.endsWith(".zip") -> extractZipMetadata(content)
            else -> {
                // Si no podemos determinar el formato por el nombre, intentamos todos
                extractWheelMetadata(content)
                    ?: extractEggMetadata(content)
                    ?: extractSdistMetadata(content)
                    ?: extractZipMetadata(content)
            }
        }
    }

    /**
     * Extrae metadatos de un archivo wheel (.whl)
     */
    private fun extractWheelMetadata(content: ByteArray): Map<String, String>? {
        try {
            ZipInputStream(ByteArrayInputStream(content)).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    // En wheels, los metadatos están en *.dist-info/METADATA
                    if (entry.name.contains(".dist-info/METADATA")) {
                        return parseMetadataFile(zipStream)
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (e: Exception) {
            // Si hay error, retornamos null
        }
        return null
    }

    /**
     * Extrae metadatos de un archivo egg (.egg)
     */
    private fun extractEggMetadata(content: ByteArray): Map<String, String>? {
        try {
            ZipInputStream(ByteArrayInputStream(content)).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    // En eggs, los metadatos están en EGG-INFO/PKG-INFO
                    if (entry.name.contains("EGG-INFO/PKG-INFO")) {
                        return parseMetadataFile(zipStream)
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) { // Suppressed unused parameter
            // Si hay error, retornamos null
        }
        return null
    }

    /**
     * Extrae metadatos de un paquete source distribution (.tar.gz)
     */
    private fun extractSdistMetadata(content: ByteArray): Map<String, String>? {
        try {
            GZIPInputStream(ByteArrayInputStream(content)).use { gzipStream ->
                return extractFileFromTarStream(gzipStream) { entryName ->
                    entryName.endsWith("PKG-INFO") || entryName.endsWith("METADATA")
                }?.let { parseMetadataContent(it) }
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) { // Suppressed unused parameter
            // Si hay error, retornamos null
        }
        return null
    }

    /**
     * Extrae metadatos de un paquete zip
     */
    private fun extractZipMetadata(content: ByteArray): Map<String, String>? {
        try {
            ZipInputStream(ByteArrayInputStream(content)).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    // Buscar archivos de metadatos en cualquier ubicación
                    if (entry.name.endsWith("PKG-INFO") || entry.name.endsWith("METADATA")) {
                        return parseMetadataFile(zipStream)
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) { // Suppressed unused parameter
            // Si hay error, retornamos null
        }
        return null
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
     * Parsea un archivo de metadatos de Python desde un stream
     */
    private fun parseMetadataFile(stream: ZipInputStream): Map<String, String> {
        val reader = BufferedReader(InputStreamReader(stream))
        val content = reader.readText()
        return parseMetadataContent(content)
    }

    /**
     * Parsea el contenido de un archivo de metadatos de Python
     */
    private fun parseMetadataContent(content: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        var currentKey: String? = null
        var currentValue = StringBuilder()

        // Formato RFC822-style (como email headers)
        for (line in content.lineSequence()) {
            if (line.startsWith(" ") && currentKey != null) {
                // Línea de continuación
                currentValue.append("\n").append(line.trimStart())
            } else if (line.contains(":")) {
                // Si teníamos un key anterior, lo guardamos
                if (currentKey != null) {
                    metadata[currentKey] = currentValue.toString().trim()
                }

                // Nueva entrada de metadata
                val parts = line.split(":", limit = 2)
                currentKey = parts[0].trim()
                currentValue = StringBuilder(parts[1].trim())
            } else if (line.isBlank() && currentKey != null) {
                // Línea en blanco, fin del valor actual
                metadata[currentKey] = currentValue.toString().trim()
                currentKey = null
                currentValue = StringBuilder()
            }
        }

        // Guardar el último par clave-valor si existe
        if (currentKey != null) {
            metadata[currentKey] = currentValue.toString().trim()
        }

        return metadata
    }

    /**
     * Extrae los clasificadores de los metadatos
     */
    private fun extractClassifiers(metadata: Map<String, String>): List<String> {
        val classifiers = mutableListOf<String>()

        // Los clasificadores pueden estar en entradas múltiples o en una sola con \n
        metadata.forEach { (key, value) ->
            if (key == "Classifier" || key == "Classifiers") {
                classifiers.addAll(value.split("\n").map { it.trim() })
            } else if (key.startsWith("Classifier")) {
                classifiers.add(value.trim())
            }
        }

        return classifiers
    }

    /**
     * Parsea una declaración de dependencia en formato Requires-Dist
     */
    private fun parseRequiresDist(requiresDistStr: String): ArtifactDependency? {
        // Formato: "package (>=1.0); extra == 'dev'"
        try {
            val parts = requiresDistStr.split(";").map { it.trim() }
            val packagePart = parts[0]

            // Extraer nombre y restricción de versión
            val nameAndVersion = packagePart.split("(", limit = 2)
            val packageName = nameAndVersion[0].trim()
            val versionConstraint = if (nameAndVersion.size > 1) {
                nameAndVersion[1].trimEnd(')').trim()
            } else null

            // Determinar si es opcional y el scope
            var isOptional = false
            var scope: String? = "runtime"

            if (parts.size > 1) {
                val marker = parts[1]
                if (marker.contains("extra")) {
                    // Ejemplo: extra == 'dev'
                    val extraMatch = Regex("""extra\s*==\s*['"]([^'"]+)['"]""").find(marker)
                    if (extraMatch != null) {
                        scope = extraMatch.groupValues[1]
                    }
                }
                if (marker.contains("optional")) {
                    isOptional = true
                }
            }

            return ArtifactDependency(
                coordinates = ArtifactCoordinates(
                    group = ArtifactGroup.NONE,
                    name = packageName,
                    version = ArtifactVersion("0.0.0") // Placeholder, la versión real está en versionConstraint
                ),
                scope = scope,
                isOptional = isOptional,
                versionConstraint = versionConstraint
            )
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) { // Suppressed unused parameter
            return null
        }
    }

    /**
     * Parsea una declaración de dependencia en formato antiguo
     */
    private fun parseRequiresLegacy(requiresStr: String): ArtifactDependency? {
        try {
            // Formato: "package (>=1.0)"
            val nameAndVersion = requiresStr.split("(", limit = 2)
            val packageName = nameAndVersion[0].trim()
            val versionConstraint = if (nameAndVersion.size > 1) {
                nameAndVersion[1].trimEnd(')').trim()
            } else null

            return ArtifactDependency(
                coordinates = ArtifactCoordinates(
                    group = ArtifactGroup.NONE,
                    name = packageName,
                    version = ArtifactVersion("0.0.0") // Placeholder, la versión real está en versionConstraint
                ),
                scope = "runtime",
                isOptional = false,
                versionConstraint = versionConstraint
            )
        } catch (e: Exception) { // Catching generic Exception, consider more specific if possible
            return null
        }
    }

    /**
     * Calcula los checksums del contenido del artefacto
     */
    private fun calculateChecksums(content: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Calcular SHA-256 (usado por PyPI moderno)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(content)
        result["SHA-256"] = sha256.joinToString("") { "%02x".format(it) }

        // Calcular MD5 (usado históricamente)
        val md5 = MessageDigest.getInstance("MD5").digest(content)
        result["MD5"] = md5.joinToString("") { "%02x".format(it) }

        return result
    }
}