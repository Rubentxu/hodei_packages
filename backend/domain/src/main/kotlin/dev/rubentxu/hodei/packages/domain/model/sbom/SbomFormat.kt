package dev.rubentxu.hodei.packages.domain.model.sbom

/**
 * Enum que representa los formatos soportados para documentos SBOM (Software Bill of Materials).
 * Los formatos comunes incluyen CycloneDX y SPDX que son estándares de la industria.
 */
enum class SbomFormat(val description: String) {
    CYCLONE_DX("CycloneDX"),
    SPDX("SPDX");

    companion object {
        /**
         * Busca un formato SBOM por su nombre (insensible a mayúsculas/minúsculas).
         * @param name Nombre del formato a buscar
         * @return El formato SBOM correspondiente o null si no se encuentra
         */
        fun fromString(name: String): SbomFormat? {
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                values().find { it.name.equals(name, ignoreCase = true) }
            }
        }
    }
}
