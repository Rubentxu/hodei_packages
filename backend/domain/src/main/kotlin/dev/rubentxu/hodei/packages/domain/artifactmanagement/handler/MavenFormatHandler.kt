package dev.rubentxu.hodei.packages.domain.artifactmanagement.handler

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.artifactmanagement.ports.FormatHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import java.io.InputStream
import java.security.MessageDigest

/**
 * Implementación del FormatHandler para artefactos Maven (JAR, WAR, etc.)
 * Utiliza un enfoque híbrido para extraer metadatos:
 * 1. Intenta extraer del contenido del JAR (pom.xml)
 * 2. Si falla, infiere del nombre del archivo
 * 3. Si falta información, utiliza metadatos proporcionados externamente
 */
class MavenFormatHandler : FormatHandler {

    override fun extractCoordinates(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?
    ): Result<Pair<ArtifactCoordinates, MetadataSource>> {
        // 1. Intentar extraer coordenadas del contenido (pom.xml dentro del JAR)
        try {
            val pomContent = extractPomFromJar(content)
            if (pomContent != null) {
                val coordinates = extractCoordinatesFromPom(pomContent)
                return Result.success(Pair(coordinates, MetadataSource.CONTENT_EXTRACTED))
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Intentar inferir del nombre del archivo
        val filenameRegex = Regex("""([\w.-]+)-([\w.-]+)-([\d.]+)(?:-SNAPSHOT)?\.(jar|war|ear)""")
        val match = filenameRegex.matchEntire(filename)
        if (match != null) {
            val (groupId, artifactId, version) = match.destructured
            val coordinates = ArtifactCoordinates(
                group = ArtifactGroup(groupId),
                name = artifactId,
                version = ArtifactVersion(version)
            )
            return Result.success(Pair(coordinates, MetadataSource.FILENAME_INFERRED))
        }

        // 3. Último recurso: usar metadatos proporcionados
        if (providedMetadata != null) {
            val groupId = providedMetadata["groupId"]
            val artifactId = providedMetadata["artifactId"]
            val version = providedMetadata["version"]

            if (groupId != null && artifactId != null && version != null) {
                val coordinates = ArtifactCoordinates(
                    group = ArtifactGroup(groupId),
                    name = artifactId,
                    version = ArtifactVersion(version)
                )
                return Result.success(Pair(coordinates, MetadataSource.USER_PROVIDED))
            }
        }

        // Si todas las estrategias fallan
        return Result.failure(IllegalArgumentException(
            "No se pudieron extraer coordenadas del artefacto Maven: $filename. " +
            "Debe proporcionar explícitamente groupId, artifactId y version."
        ))
    }

    override fun extractMetadataWithSources(
        filename: String,
        content: ByteArray,
        providedMetadata: Map<String, String>?,
        artifactId: ArtifactId,
        userId: UserId
    ): Result<ArtifactMetadataWithSources> {
        // Resultados iniciales con valores por defecto - se irán sobrescribiendo
        var description: MetadataWithSource<String?>? = null
        var licenses: MetadataWithSource<List<String>?>? = null
        var homepageUrl: MetadataWithSource<String?>? = null
        var repositoryUrl: MetadataWithSource<String?>? = null
        val sizeInBytes: MetadataWithSource<Long?>? = MetadataWithSource(content.size.toLong(), MetadataSource.SYSTEM_GENERATED)

        // Calcular checksums
        val checksums = calculateChecksums(content)
        val checksumsWithSource: MetadataWithSource<Map<String, String>?>? = MetadataWithSource(checksums, MetadataSource.SYSTEM_GENERATED)


        // 1. Intentar extraer metadatos del POM.xml en el JAR
        try {
            val pomContent = extractPomFromJar(content)
            if (pomContent != null) {
                val pomDoc = parsePomXml(pomContent)

                // Extraer datos del POM
                val pomDescription = getElementText(pomDoc, "description")
                if (pomDescription != null) {
                    description = MetadataWithSource(pomDescription, MetadataSource.CONTENT_EXTRACTED)
                }

                val pomUrl = getElementText(pomDoc, "url")
                if (pomUrl != null) {
                    homepageUrl = MetadataWithSource(pomUrl, MetadataSource.CONTENT_EXTRACTED)
                }

                // Extraer licencias (múltiples)
                val licenseNodes = getElements(pomDoc, "licenses/license/name")
                if (licenseNodes.isNotEmpty()) {
                    val licenseList = licenseNodes.map { it.textContent.trim() }
                    licenses = MetadataWithSource(licenseList, MetadataSource.CONTENT_EXTRACTED)
                }

                // Extraer URL del repositorio
                val scmUrl = getElementText(pomDoc, "scm/url")
                if (scmUrl != null) {
                    repositoryUrl = MetadataWithSource(scmUrl, MetadataSource.CONTENT_EXTRACTED)
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. No hay metadatos que se puedan inferir confiablemente del nombre de archivo
        // excepto posiblemente el tamaño que ya establecimos

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

            if (repositoryUrl == null && providedMetadata["repositoryUrl"] != null) {
                repositoryUrl = MetadataWithSource(providedMetadata["repositoryUrl"], MetadataSource.USER_PROVIDED)
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
                checksums = checksumsWithSource // Añadir checksums
            )
        )
    }

    override fun determinePackagingType(
        filename: String,
        content: ByteArray
    ): Result<Pair<String, MetadataSource>> {
        // 1. Intentar extraer del contenido (POM)
        try {
            val pomContent = extractPomFromJar(content)
            if (pomContent != null) {
                val pomDoc = parsePomXml(pomContent)
                val packaging = getElementText(pomDoc, "packaging")
                if (packaging != null) {
                    return Result.success(Pair(packaging, MetadataSource.CONTENT_EXTRACTED))
                }
            }
        } catch (e: Exception) {
            // Continuar con siguiente método si falla
        }

        // 2. Inferir de la extensión del archivo
        val extension = filename.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty()) {
            return when (extension) {
                "jar" -> Result.success(Pair("jar", MetadataSource.FILENAME_INFERRED))
                "war" -> Result.success(Pair("war", MetadataSource.FILENAME_INFERRED))
                "ear" -> Result.success(Pair("ear", MetadataSource.FILENAME_INFERRED))
                "pom" -> Result.success(Pair("pom", MetadataSource.FILENAME_INFERRED))
                else -> Result.success(Pair(extension, MetadataSource.FILENAME_INFERRED))
            }
        }

        // 3. Si todo falla, asumir JAR por defecto para artefactos Maven
        return Result.success(Pair("jar", MetadataSource.SYSTEM_GENERATED))
    }

    override fun generateDescriptor(artifact: Artifact): Result<String> {
        val coordinates = artifact.coordinates
        val meta = artifact.metadata

        val pomXml = """
            <project xmlns="http://maven.apache.org/POM/4.0.0" 
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>${coordinates.group.value}</groupId>
                <artifactId>${coordinates.name}</artifactId>
                <version>${coordinates.version.value}</version>
                <packaging>${artifact.packagingType ?: "jar"}</packaging>
                
                ${meta.description?.let { "<description>$it</description>" } ?: ""}
                ${meta.homepageUrl?.let { "<url>$it</url>" } ?: ""}
                
                ${meta.licenses?.let { licenses ->
                    if (licenses.isNotEmpty()) {
                        """
                        <licenses>
                            ${licenses.joinToString("\n") { 
                                """
                                <license>
                                    <name>$it</name>
                                </license>
                                """.trimIndent()
                            }}
                        </licenses>
                        """
                    } else ""
                } ?: ""}
                
                ${meta.repositoryUrl?.let {
                    """
                    <scm>
                        <url>$it</url>
                    </scm>
                    """
                } ?: ""}
            </project>
        """.trimIndent()

        return Result.success(pomXml)
    }

    override fun validateMetadataConsistency(
        extracted: ArtifactMetadataWithSources,
        provided: Map<String, String>
    ): Result<Boolean> {
        val inconsistencies = mutableListOf<String>()

        // Comparamos coordenadas solo si se extrajeron del contenido
        val groupIdFromPom = provided["groupId"]
        val artifactIdFromPom = provided["artifactId"]
        val versionFromPom = provided["version"]

        // Verificar solo inconsistencias entre metadatos extraídos del contenido y proporcionados por el usuario
        // Solo reportar error si el dato extraído del contenido no coincide con el proporcionado por el usuario
        extracted.description?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["description"] != null &&
                it.value != provided["description"]) {
                inconsistencies.add("La descripción proporcionada no coincide con la del archivo POM")
            }
        }

        extracted.homepageUrl?.let {
            if (it.source == MetadataSource.CONTENT_EXTRACTED &&
                provided["homepage"] != null &&
                it.value != provided["homepage"]) {
                inconsistencies.add("La URL de la página de inicio proporcionada no coincide con la del archivo POM")
            }
        }

        // Permitimos al usuario proporcionar información adicional, pero no contradictoria

        return if (inconsistencies.isEmpty()) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException(
                "Inconsistencias encontradas entre metadatos proporcionados y extraídos: ${inconsistencies.joinToString("; ")}"
            ))
        }
    }

    override fun extractDependencies(content: ByteArray): Result<List<ArtifactDependency>> {
        TODO("Not yet implemented")
    }

    // ===== Métodos de utilidad =====

    /**
     * Calcula los checksums del contenido del artefacto (SHA-1, MD5 son comunes para Maven)
     */
    private fun calculateChecksums(content: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Calcular SHA-1
        val sha1 = MessageDigest.getInstance("SHA-1").digest(content)
        result["SHA-1"] = sha1.joinToString("") { "%02x".format(it) }

        // Calcular MD5
        val md5 = MessageDigest.getInstance("MD5").digest(content)
        result["MD5"] = md5.joinToString("") { "%02x".format(it) }
        
        // Calcular SHA-256 (opcional, pero bueno tenerlo)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(content)
        result["SHA-256"] = sha256.joinToString("") { "%02x".format(it) }

        return result
    }

    /**
     * Extrae el contenido del pom.xml de un archivo JAR
     */
    private fun extractPomFromJar(jarContent: ByteArray): String? {
        ZipInputStream(ByteArrayInputStream(jarContent)).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                // Buscar archivos POM en ubicaciones estándar
                if (entry.name.endsWith("pom.xml") ||
                    entry.name.contains("/META-INF/maven/") && entry.name.endsWith("pom.xml")) {
                    return zipStream.readBytes().toString(Charsets.UTF_8)
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
        return null
    }

    /**
     * Parsea un XML de POM a un Document de DOM
     */
    private fun parsePomXml(pomContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val inputStream: InputStream = ByteArrayInputStream(pomContent.toByteArray())
        return builder.parse(inputStream)
    }

    /**
     * Extrae las coordenadas Maven de un documento POM
     */
    private fun extractCoordinatesFromPom(pomContent: String): ArtifactCoordinates {
        val pomDoc = parsePomXml(pomContent)

        val groupId = getElementText(pomDoc, "groupId")
            ?: getElementText(pomDoc, "parent/groupId")
            ?: throw IllegalArgumentException("No se encontró groupId en el POM")

        val artifactId = getElementText(pomDoc, "artifactId")
            ?: throw IllegalArgumentException("No se encontró artifactId en el POM")

        val version = getElementText(pomDoc, "version")
            ?: getElementText(pomDoc, "parent/version")
            ?: throw IllegalArgumentException("No se encontró version en el POM")

        return ArtifactCoordinates(
            group = ArtifactGroup(groupId),
            name = artifactId,
            version = ArtifactVersion(version)
        )
    }

    /**
     * Obtiene el texto de un elemento XML del POM usando una ruta XPath simple
     */
    private fun getElementText(doc: Document, path: String): String? {
        val parts = path.split("/")
        var current: org.w3c.dom.Node = doc.documentElement

        for (part in parts) {
            val childNodes = current.childNodes
            var found = false

            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE && node.nodeName == part) {
                    current = node
                    found = true
                    break
                }
            }

            if (!found) return null
        }

        return current.textContent?.takeIf { it.isNotBlank() }
    }

    /**
     * Obtiene elementos XML del POM usando una ruta XPath simple
     */
    private fun getElements(doc: Document, path: String): List<org.w3c.dom.Node> {
        val parts = path.split("/")
        var currentNodes = listOf<org.w3c.dom.Node>(doc.documentElement)
        var result = listOf<org.w3c.dom.Node>()

        for (partIndex in parts.indices) {
            val part = parts[partIndex]
            val isLastPart = partIndex == parts.size - 1
            val nextNodes = mutableListOf<org.w3c.dom.Node>()

            for (current in currentNodes) {
                val childNodes = current.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i)
                    if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE && node.nodeName == part) {
                        if (isLastPart) {
                            result += node
                        } else {
                            nextNodes.add(node)
                        }
                    }
                }
            }

            if (!isLastPart) {
                currentNodes = nextNodes
                if (currentNodes.isEmpty()) break
            }
        }

        return result
    }
}