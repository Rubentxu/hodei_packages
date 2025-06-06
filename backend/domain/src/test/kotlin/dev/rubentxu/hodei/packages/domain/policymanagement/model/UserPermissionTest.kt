package dev.rubentxu.hodei.packages.domain.policymanagement.model

import dev.rubentxu.hodei.packages.domain.model.permission.UserPermission
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class UserPermissionTest : StringSpec({
    
    "should create a valid repository-specific permission" {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        val repositoryId = UUID.randomUUID()
        val grantedBy = UUID.randomUUID()
        val now = Instant.now()
        val expiresAt = now.plus(30, ChronoUnit.DAYS)
        
        val permission = UserPermission(
            id = id,
            userId = userId,
            roleId = roleId,
            registryId = repositoryId,
            grantedBy = grantedBy,
            grantedAt = now,
            expiresAt = expiresAt
        )
        
        permission.id shouldBe id
        permission.userId shouldBe userId
        permission.roleId shouldBe roleId
        permission.registryId shouldBe repositoryId
        permission.grantedBy shouldBe grantedBy
        permission.grantedAt shouldBe now
        permission.expiresAt shouldBe expiresAt
    }
    
    "should create a valid global permission" {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        val grantedBy = UUID.randomUUID()
        val now = Instant.now()
        
        val permission = UserPermission(
            id = id,
            userId = userId,
            roleId = roleId,
            registryId = null, // Global permission
            grantedBy = grantedBy,
            grantedAt = now,
            expiresAt = null     // Never expires
        )
        
        permission.id shouldBe id
        permission.userId shouldBe userId
        permission.roleId shouldBe roleId
        permission.registryId shouldBe null
        permission.grantedBy shouldBe grantedBy
        permission.grantedAt shouldBe now
        permission.expiresAt shouldBe null
    }
    
    "should create global permission using factory method" {
        val userId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        val grantedBy = UUID.randomUUID()
        
        val permission = UserPermission.Companion.createGlobalPermission(
            userId = userId,
            roleId = roleId,
            grantedBy = grantedBy
        )
        
        permission.userId shouldBe userId
        permission.roleId shouldBe roleId
        permission.registryId shouldBe null
        permission.grantedBy shouldBe grantedBy
        permission.expiresAt shouldBe null
    }
    
    "should create repository permission using factory method" {
        val userId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        val repositoryId = UUID.randomUUID()
        val grantedBy = UUID.randomUUID()
        val expiresAt = Instant.now().plus(60, ChronoUnit.DAYS)
        
        val permission = UserPermission.Companion.createRepositoryPermission(
            userId = userId,
            roleId = roleId,
            repositoryId = repositoryId,
            grantedBy = grantedBy,
            expiresAt = expiresAt
        )
        
        permission.userId shouldBe userId
        permission.roleId shouldBe roleId
        permission.registryId shouldBe repositoryId
        permission.grantedBy shouldBe grantedBy
        permission.expiresAt shouldBe expiresAt
    }
    
    "should check if permission is active" {
        val futureExpiration = Instant.now().plus(10, ChronoUnit.DAYS)
        val pastExpiration = Instant.now().minus(1, ChronoUnit.DAYS)
        
        val activePermission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = UUID.randomUUID(),
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now(),
            expiresAt = futureExpiration
        )
        
        val expiredPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = UUID.randomUUID(),
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now().minus(10, ChronoUnit.DAYS),
            expiresAt = pastExpiration
        )
        
        val permanentPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = UUID.randomUUID(),
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now(),
            expiresAt = null
        )
        
        activePermission.isActive() shouldBe true
        expiredPermission.isActive() shouldBe false
        permanentPermission.isActive() shouldBe true
    }
    
    "should check if permission applies to repository" {
        val repoId1 = UUID.randomUUID()
        val repoId2 = UUID.randomUUID()
        
        val specificPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = repoId1,
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now(),
            expiresAt = null
        )
        
        val globalPermission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = null,
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now(),
            expiresAt = null
        )
        
        specificPermission.appliesTo(repoId1) shouldBe true
        specificPermission.appliesTo(repoId2) shouldBe false
        globalPermission.appliesTo(repoId1) shouldBe true
        globalPermission.appliesTo(repoId2) shouldBe true
    }
    
    "should update expiration date" {
        val permission = UserPermission(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            roleId = UUID.randomUUID(),
            registryId = UUID.randomUUID(),
            grantedBy = UUID.randomUUID(),
            grantedAt = Instant.now(),
            expiresAt = Instant.now().plus(10, ChronoUnit.DAYS)
        )
        
        val newExpiration = Instant.now().plus(30, ChronoUnit.DAYS)
        val updatedPermission = permission.withNewExpiration(newExpiration)
        
        updatedPermission.expiresAt shouldBe newExpiration
        // Original permission should remain unchanged (immutability)
        permission.expiresAt shouldBe permission.expiresAt
    }
    
    "should throw exception when expiration date is before grant date" {
        val now = Instant.now()
        val pastDate = now.minus(1, ChronoUnit.DAYS)
        
        val exception = shouldThrow<IllegalArgumentException> {
            UserPermission(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                roleId = UUID.randomUUID(),
                registryId = UUID.randomUUID(),
                grantedBy = UUID.randomUUID(),
                grantedAt = now,
                expiresAt = pastDate
            )
        }
        
        exception.message shouldBe "Expiration date must be after grant date"
    }
})