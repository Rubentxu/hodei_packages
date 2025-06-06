package dev.rubentxu.hodei.packages.domain.artifacmanagement

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.Artifact
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactCoordinates
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.ArtifactStatus
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.MerkleRootHash
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.SignatureId
import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.UserId
import dev.rubentxu.hodei.packages.domain.integrityverification.sbom.model.SbomId
import dev.rubentxu.hodei.packages.domain.policymanagement.model.PolicyId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class ArtifactTest : StringSpec({

    "should create a valid artifact with minimal required fields" {
        val artifactId = ArtifactId("art-123")
        val userId = UserId("user-abc")
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(
            group = "org.example",
            name = "example-lib",
            version = "1.0.0"
        )

        val artifact = Artifact(
            id = artifactId,
            coordinates = coordinates,
            createdBy = userId,
            createdAt = now
        )

        artifact.id shouldBe artifactId
        artifact.coordinates shouldBe coordinates
        artifact.coordinates.group shouldBe "org.example"
        artifact.coordinates.name shouldBe "example-lib"
        artifact.coordinates.version shouldBe "1.0.0"
        artifact.createdBy shouldBe userId
        artifact.createdAt shouldBe now
        artifact.status shouldBe ArtifactStatus.ACTIVE // Default value
        artifact.sbomIds shouldBe emptyList<SbomId>() // Default value/ Default value
        artifact.signatureIds shouldBe emptyList() // Default value
        artifact.merkleRoot shouldBe null // Default value
        artifact.policies shouldBe emptyList<PolicyId>() // Default value
    }

    "should create a valid artifact with all fields populated" {
        val artifactId = ArtifactId("art-456")
        val userId = UserId("user-xyz")
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(
            group = "com.test",
            name = "another-lib",
            version = "2.1.3"
        )
        val sbomId1 = SbomId("sbom-001")
        val signatureId1 = SignatureId("sig-abc")
        val merkleRootHash = MerkleRootHash("a1b2c3d4e5f600112233445566778899aabbccddeeff")
        val policyId1 = PolicyId("policy-xzy")

        val artifact = Artifact(
            id = artifactId,
            coordinates = coordinates,
            createdBy = userId,
            createdAt = now,
            sbomIds = listOf(sbomId1),
            signatureIds = listOf(signatureId1),
            merkleRoot = merkleRootHash,
            policies = listOf(policyId1),
            status = ArtifactStatus.DEPRECATED
        )

        artifact.id shouldBe artifactId
        artifact.coordinates shouldBe coordinates
        artifact.createdBy shouldBe userId
        artifact.createdAt shouldBe now
        artifact.status shouldBe ArtifactStatus.DEPRECATED
        artifact.sbomIds shouldBe listOf(sbomId1)
        artifact.signatureIds shouldBe listOf(signatureId1)
        artifact.merkleRoot shouldBe merkleRootHash
        artifact.policies shouldBe listOf(policyId1)
    }

    // Los tests anteriores que validaban propiedades como registryId, registryType, fileSize, sha256, metadata
    // directamente en el constructor de Artifact ya no son aplicables debido al cambio en el modelo de dominio.
    // Esas validaciones, si aún son necesarias, deberían ocurrir en otras capas o servicios
    // que manejen esos datos antes de crear el objeto Artifact de dominio.
    // Por ejemplo, la validación del formato de versión semántica (SemVer) podría estar en ArtifactCoordinates
    // o en un Value Object específico para la versión si se requiere una lógica más compleja.
})