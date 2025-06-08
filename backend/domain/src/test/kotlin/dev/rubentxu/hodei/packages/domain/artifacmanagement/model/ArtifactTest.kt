package dev.rubentxu.hodei.packages.domain.artifacmanagement.model

import dev.rubentxu.hodei.packages.domain.artifactmanagement.model.*
import dev.rubentxu.hodei.packages.domain.identityaccess.model.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class ArtifactTest : StringSpec({

    val testUserId = UserId("test-user-123")
    val testArtifactId = ArtifactId("art-common-id")
    val testContentHash = ContentHash("sha256-commondummyhashvalue")

    "should create a valid artifact with minimal required fields" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(
            name = "minimal-lib",
            version = ArtifactVersion("0.1.0")
        )

        val artifactMetadata = ArtifactMetadata(
            id = testArtifactId,
            createdBy = testUserId,
            createdAt = now,
            // updatedAt defaults to createdAt
            sizeInBytes = 0L // Explicitly setting for clarity in minimal case
        )

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            metadata = artifactMetadata
            // status defaults to ACTIVE
            // tags, packagingType, sizeInBytes (on Artifact), dependencies default to null
        )

        artifact.id shouldBe testArtifactId
        artifact.contentHash shouldBe testContentHash
        artifact.coordinates shouldBe coordinates
        artifact.coordinates.group shouldBe ArtifactGroup.NONE
        artifact.coordinates.name shouldBe "minimal-lib"
        artifact.coordinates.version.value shouldBe "0.1.0"
        artifact.metadata.createdBy shouldBe testUserId
        artifact.metadata.createdAt shouldBe now
        artifact.metadata.updatedAt shouldBe now // Default behavior
        artifact.status shouldBe ArtifactStatus.ACTIVE
        artifact.tags shouldBe null
        artifact.packagingType shouldBe null
        artifact.sizeInBytes shouldBe null
        artifact.dependencies shouldBe null
        artifact.metadata.description shouldBe null
        artifact.metadata.licenses shouldBe null
        artifact.metadata.homepageUrl shouldBe null
        artifact.metadata.repositoryUrl shouldBe null
        artifact.metadata.checksums shouldBe null
        artifact.metadata.sizeInBytes shouldBe 0L
    }

    "should create a valid artifact with all fields populated" {
        val now = Instant.now()
        val later = now.plusSeconds(60)
        val coordinates = ArtifactCoordinates(
            group = ArtifactGroup("com.test.fully"),
            name = "populated-lib",
            version = ArtifactVersion("3.0.0-alpha"),
            classifier = ArtifactClassifier("full"),
            extension = ArtifactExtension("zip")
        )
        val tags = listOf("alpha", "full-feature")
        val packagingType = "application/zip"
        val artifactSizeInBytes = 2048L
        val metadataSizeInBytes = 2050L // Can be slightly different
        val description = "A fully populated test artifact."
        val licenses = listOf("MIT", "Apache-2.0")
        val homepageUrl = "https://example.com/populated-lib"
        val repositoryUrl = "https://github.com/test/populated-lib"
        val checksums = mapOf("SHA-1" to "checksumsha1-full", "MD5" to "checksummd5-full")
        val dependencies = listOf(
            ArtifactCoordinates(name = "dependency-X", version = ArtifactVersion("1.0")),
            ArtifactCoordinates(
                group = ArtifactGroup("org.dep"),
                name = "dependency-Y",
                version = ArtifactVersion("2.2")
            )
        )

        val artifactMetadata = ArtifactMetadata(
            id = testArtifactId,
            createdBy = testUserId,
            createdAt = now,
            updatedAt = later,
            description = description,
            licenses = licenses,
            homepageUrl = homepageUrl,
            repositoryUrl = repositoryUrl,
            sizeInBytes = metadataSizeInBytes,
            checksums = checksums
        )

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            tags = tags,
            packagingType = packagingType,
            sizeInBytes = artifactSizeInBytes,
            status = ArtifactStatus.PRE_RELEASE,
            metadata = artifactMetadata,
            dependencies = dependencies
        )

        artifact.id shouldBe testArtifactId
        artifact.contentHash shouldBe testContentHash
        artifact.coordinates shouldBe coordinates
        artifact.coordinates.classifier shouldBe ArtifactClassifier("full")
        artifact.coordinates.extension shouldBe ArtifactExtension("zip")
        artifact.tags shouldBe tags
        artifact.packagingType shouldBe packagingType
        artifact.sizeInBytes shouldBe artifactSizeInBytes
        artifact.status shouldBe ArtifactStatus.PRE_RELEASE
        artifact.dependencies shouldBe dependencies

        val meta = artifact.metadata
        meta.id shouldBe testArtifactId
        meta.createdBy shouldBe testUserId
        meta.createdAt shouldBe now
        meta.updatedAt shouldBe later
        meta.description shouldBe description
        meta.licenses shouldBe licenses
        meta.homepageUrl shouldBe homepageUrl
        meta.repositoryUrl shouldBe repositoryUrl
        meta.sizeInBytes shouldBe metadataSizeInBytes
        meta.checksums shouldBe checksums
    }

    "should allow artifact coordinates with NONE values for optional parts" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(
            group = ArtifactGroup.NONE, // Explicitly NONE
            name = "npm-style-package",
            version = ArtifactVersion("1.2.3"),
            classifier = ArtifactClassifier.NONE, // Explicitly NONE
            extension = ArtifactExtension.NONE // Explicitly NONE
        )
        val artifactMetadata = ArtifactMetadata(testArtifactId, testUserId, now, now, sizeInBytes = 100L)

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            metadata = artifactMetadata
        )

        artifact.coordinates.group shouldBe ArtifactGroup.NONE
        artifact.coordinates.classifier shouldBe ArtifactClassifier.NONE
        artifact.coordinates.extension shouldBe ArtifactExtension.NONE
        artifact.coordinates.name shouldBe "npm-style-package" // Name is always required
    }

    "should allow creation with a specific non-default status like PENDING" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(name = "pending-artifact", version = ArtifactVersion("0.0.1"))
        val artifactMetadata = ArtifactMetadata(testArtifactId, testUserId, now, now, sizeInBytes = 50L)

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            status = ArtifactStatus.PENDING,
            metadata = artifactMetadata
        )
        artifact.status shouldBe ArtifactStatus.PENDING
    }

    "should allow creation with an empty list for tags" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(name = "no-tags-artifact", version = ArtifactVersion("1.0"))
        val artifactMetadata = ArtifactMetadata(testArtifactId, testUserId, now, now, sizeInBytes = 120L)

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            tags = emptyList(), // Explicit empty list
            metadata = artifactMetadata
        )
        artifact.tags shouldNotBe null
        artifact.tags?.shouldBeEmpty()
    }

    "should allow creation with an empty list for metadata licenses" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(name = "no-licenses-artifact", version = ArtifactVersion("1.0"))
        val artifactMetadata = ArtifactMetadata(
            id = testArtifactId,
            createdBy = testUserId,
            createdAt = now,
            updatedAt = now,
            licenses = emptyList(), // Explicit empty list
            sizeInBytes = 130L
        )

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            metadata = artifactMetadata
        )
        artifact.metadata.licenses shouldNotBe null
        artifact.metadata.licenses?.shouldBeEmpty()
    }

    "should allow creation with empty map for metadata checksums" {
        val now = Instant.now()
        val coordinates = ArtifactCoordinates(name = "no-checksums-artifact", version = ArtifactVersion("1.0"))
        val artifactMetadata = ArtifactMetadata(
            id = testArtifactId,
            createdBy = testUserId,
            createdAt = now,
            updatedAt = now,
            checksums = emptyMap(), // Explicit empty map
            sizeInBytes = 140L
        )

        val artifact = Artifact(
            id = testArtifactId,
            contentHash = testContentHash,
            coordinates = coordinates,
            metadata = artifactMetadata
        )
        artifact.metadata.checksums shouldNotBe null
        artifact.metadata.checksums?.shouldBeEmpty()
    }


    "ArtifactMetadata should default updatedAt to createdAt if not specified" {
        val now = Instant.now()
        val metadata = ArtifactMetadata(
            id = testArtifactId,
            createdBy = testUserId,
            createdAt = now,
            // updatedAt is not provided, should default to createdAt
            sizeInBytes = 10L
        )
        metadata.updatedAt shouldBe now
        metadata.createdAt shouldBe now
    }

    "ContentHash should require non-blank value" {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            ContentHash("  ")
        }
        exception.message shouldBe "Content hash cannot be blank."
    }

    "ArtifactId should store value correctly" {
        val idValue = "unique-artifact-identifier-789"
        val artifactId = ArtifactId(idValue)
        artifactId.value shouldBe idValue
    }

    "ArtifactVersion should require non-blank value" {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            ArtifactVersion("")
        }
        exception.message shouldBe "Artifact version cannot be blank"
    }
})