package dev.rubentxu.hodei.packages.domain.artifactmanagement.common.model

// SbomFormat ha sido eliminado de aquí y se usa dev.rubentxu.hodei.packages.domain.model.sbom.SbomFormat
enum class SignatureType { SIGSTORE, GPG, X509 }
enum class VexStatus { AFFECTED, NOT_AFFECTED, FIXED, UNDER_INVESTIGATION } 