package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

/**
 * Enumeration representing the different formats available for Software Bill of Materials.
 */
enum class SbomFormat(val officialName: String, val description: String) {
    /**
     * CycloneDX is a lightweight software bill of materials (SBOM) standard designed for application security contexts
     * and supply chain component analysis.
     */
    CYCLONE_DX("CycloneDX", "CycloneDX SBOM Format"),

    /**
     * Software Package Data Exchange (SPDX) is an open standard for communicating software bill of materials information,
     * including components, licenses, copyrights, and security references.
     */
    SPDX("SPDX", "SPDX SBOM Format");

    companion object {
        /**
         * Finds a SbomFormat by name (case insensitive).
         * It can match by enum name, official name, or description.
         *
         * @param name Name to search for.
         * @return The matching SbomFormat.
         * @throws IllegalArgumentException if no matching format is found.
         */
        @JvmStatic
        fun fromString(name: String): SbomFormat {
            val normalizedName = name.trim().lowercase()
            return entries.find {
                it.name.lowercase() == normalizedName ||
                it.officialName.lowercase() == normalizedName ||
                it.description.lowercase().contains(normalizedName)
            } ?: throw IllegalArgumentException("Unknown SbomFormat: $name")
        }

        /**
         * Safe version of fromString that returns a Result instead of throwing an exception.
         *
         * @param name Name to search for.
         * @return Result containing the SbomFormat if found, or a Failure with an exception.
         */
        @JvmStatic
        fun safeFromString(name: String): Result<SbomFormat> {
            return try {
                Result.success(fromString(name))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
