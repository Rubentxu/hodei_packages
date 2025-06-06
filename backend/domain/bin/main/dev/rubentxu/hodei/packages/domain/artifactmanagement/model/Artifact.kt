package dev.rubentxu.hodei.packages.domain.artifactmanagement.model

import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import java.time.Instant

/**
 * Represents a software artifact stored in the registry.
 * This model aims to be generic enough to accommodate artifacts from various ecosystems
 * like Maven, npm, Python (pip), Rust (cargo), Go, etc.
 *
 * @param id Unique identifier for the artifact within this registry.
 * @param coordinates The group, name, and version identifying the artifact.
 * @param createdBy Identifier of the user or process that created/uploaded this artifact entry.
 * @param createdAt Timestamp of when this artifact entry was created in the registry.
 * @param description A brief description of the artifact.
 * @param licenses List of licenses under which the artifact is distributed (e.g., SPDX IDs).
 * @param homepageUrl URL to the project's or artifact's homepage.
 * @param repositoryUrl URL to the source code repository.
 * @param tags Keywords or tags associated with the artifact for discovery.
 * @param packagingType The specific file format or packaging of the artifact (e.g., "jar", "tgz", "whl", "crate").
 * @param sizeInBytes Size of the artifact's primary file in bytes.
 * @param checksums A map of checksum algorithms to their hex-encoded hash values for the primary artifact file.
 *                  (e.g., {"SHA256": "...", "MD5": "..."}).
 * @param sbomIds List of identifiers for Software Bill of Materials (SBOMs) associated with this artifact.
 * @param signatureIds List of identifiers for digital signatures associated with this artifact.
 * @param merkleRoot Optional Merkle root hash for comprehensive integrity verification of the artifact's contents.
 * @param policies List of policy identifiers that apply to this artifact.
 * @param status The current lifecycle status of the artifact.
 * @param customProperties A flexible map for storing additional, ecosystem-specific metadata.
 */
data class Artifact(
    val id: ArtifactId,
    val coordinates: ArtifactCoordinates,
    val createdBy: UserId,
    val createdAt: Instant,
    val description: String? = null,
    val licenses: List<String>? = null,
    val homepageUrl: String? = null,
    val repositoryUrl: String? = null,
    val tags: List<String>? = null,
    val packagingType: String? = null,
    val sizeInBytes: Long? = null,
    val checksums: Map<String, String>? = null,
    val sbomIds: List<SbomId> = emptyList(),
    val signatureIds: List<SignatureId> = emptyList(),
    val merkleRoot: MerkleRootHash? = null,
    val policies: List<PolicyId> = emptyList(),
    val status: ArtifactStatus = ArtifactStatus.ACTIVE,
    val customProperties: Map<String, String>? = null
) 