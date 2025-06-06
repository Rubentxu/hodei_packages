package dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model

/**
 * Enum representing the supported formats for SBOM (Software Bill of Materials) documents.
 * Common formats include CycloneDX and SPDX, which are industry standards.
 *
 * @param description A human-readable description of the format.
 * @param officialName The official or commonly used name for the format.
 */
enum class SbomFormat(val description: String, val officialName: String) {
    CYCLONE_DX("CycloneDX SBOM Format", "CycloneDX"),
    SPDX("SPDX SBOM Format", "SPDX");
    // You could consider adding other properties here if needed, e.g.:
    // val mediaType: String,
    // val defaultFileExtension: String

    companion object {
        /**
         * Finds an SbomFormat by its enum constant name (case-insensitive),
         * official name, or if the input string is contained within its description.
         *
         * @param input The string representation of the format to find.
         * @return A Result containing the SbomFormat if found, or a failure Result with an
         *         IllegalArgumentException if no match is found.
         */
        fun fromString(input: String): Result<SbomFormat> {
            val upperInput = input.uppercase().replace("-", "_")
            return try {
                // Attempt to match by enum constant name (e.g., "CYCLONE_DX")
                Result.success(valueOf(upperInput))
            } catch (e: IllegalArgumentException) {
                // If direct match fails, search by officialName or description
                entries.find {
                    it.officialName.equals(input, ignoreCase = true) ||
                            it.description.contains(input, ignoreCase = true) ||
                            it.name.equals(upperInput, ignoreCase = true) // Fallback to enum name check
                }?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalArgumentException("Unknown SbomFormat: $input"))
            }
        }
    }
}