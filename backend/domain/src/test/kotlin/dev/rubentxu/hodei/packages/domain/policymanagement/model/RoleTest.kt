package dev.rubentxu.hodei.packages.domain.policymanagement.model


import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class RoleTest : StringSpec({
    
    "should create a valid role" {
        val id = RoleId(UUID.randomUUID())
        val createdBy = UUID.randomUUID()
        val now = Instant.now()
        
        val role = Role(
            id = id,
            name = "test-role",
            description = "Test role for unit testing",
            permissions = setOf(Permission.VIEW_REPOSITORY, Permission.DOWNLOAD_ARTIFACT),
            isSystemRole = false,
            createdBy = createdBy,
            createdAt = now,
            updatedAt = now
        )
        
        role.id shouldBe id
        role.name shouldBe "test-role"
        role.description shouldBe "Test role for unit testing"
        role.permissions shouldBe setOf(Permission.VIEW_REPOSITORY, Permission.DOWNLOAD_ARTIFACT)
        role.isSystemRole shouldBe false
        role.createdBy shouldBe createdBy
        role.createdAt shouldBe now
        role.updatedAt shouldBe now
    }
    
    "should create predefined admin role" {
        val createdBy = UUID.randomUUID()
        val role = Role.Companion.createAdminRole(createdBy)
        
        role.name shouldBe "ADMIN"
        role.isSystemRole shouldBe true
        role.permissions shouldBe Permission.ADMIN_PERMISSIONS
        role.createdBy shouldBe createdBy
    }
    
    "should create predefined contributor role" {
        val createdBy = UUID.randomUUID()
        val role = Role.Companion.createContributorRole(createdBy)
        
        role.name shouldBe "CONTRIBUTOR"
        role.isSystemRole shouldBe true
        role.permissions shouldBe Permission.CONTRIBUTOR_PERMISSIONS
        role.createdBy shouldBe createdBy
    }
    
    "should create predefined reader role" {
        val createdBy = UUID.randomUUID()
        val role = Role.Companion.createReaderRole(createdBy)
        
        role.name shouldBe "READER"
        role.isSystemRole shouldBe true
        role.permissions shouldBe Permission.READER_PERMISSIONS
        role.createdBy shouldBe createdBy
    }
    
    "should throw exception when name is blank" {
        val exception = shouldThrow<IllegalArgumentException> {
            Role(
                id =RoleId(UUID.randomUUID()),
                name = "",
                description = "Invalid role",
                permissions = setOf(Permission.VIEW_REPOSITORY),
                isSystemRole = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
        
        exception.message shouldBe "Role name cannot be blank"
    }
    
    "should throw exception when name has invalid format" {
        val exception = shouldThrow<IllegalArgumentException> {
            Role(
                id = RoleId(UUID.randomUUID()),
                name = "invalid name with spaces",
                description = "Invalid role",
                permissions = setOf(Permission.VIEW_REPOSITORY),
                isSystemRole = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
        
        exception.message shouldBe "Role name can only contain alphanumeric characters, hyphens, and underscores"
    }
    
    "should throw exception when description is too long" {
        val exception = shouldThrow<IllegalArgumentException> {
            Role(
                id = RoleId(UUID.randomUUID()),
                name = "test-role",
                description = "a".repeat(256),
                permissions = setOf(Permission.VIEW_REPOSITORY),
                isSystemRole = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
        
        exception.message shouldBe "Role description cannot exceed 255 characters"
    }
    
    "should throw exception when permissions are empty" {
        val exception = shouldThrow<IllegalArgumentException> {
            Role(
                id = RoleId(UUID.randomUUID()),
                name = "test-role",
                description = "Invalid role",
                permissions = emptySet(),
                isSystemRole = false,
                createdBy = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
        
        exception.message shouldBe "Role must have at least one permission"
    }
})