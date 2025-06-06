package dev.rubentxu.hodei.packages.domain.integrityverification.attestation.ports

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.integrityverification.attestation.model.Attestation

/**
 * Port for fetching and verifying attestations.
 * Defines the operations for retrieving and validating attestations associated with artifacts.
 */
interface AttestationPort {
    /**
     * Fetches all attestations for a given artifact ID.
     * @param artifactId The ID of the artifact
     * @return Result with the list of attestations or an error
     */
    suspend fun fetchAttestations(artifactId: ArtifactId): Result<List<Attestation>>

    /**
     * Verifies the validity of an attestation.
     * @param attestation The attestation to verify
     * @return Result with true if the attestation is valid, false otherwise, or an error
     */
    suspend fun verifyAttestation(attestation: Attestation): Result<Boolean>
}