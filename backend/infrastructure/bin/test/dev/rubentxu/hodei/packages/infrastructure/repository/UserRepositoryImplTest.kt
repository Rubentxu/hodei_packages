package dev.rubentxu.hodei.packages.infrastructure.repository

import dev.rubentxu.hodei.packages.domain.identityaccess.model.AdminUser
import dev.rubentxu.hodei.packages.infrastructure.persistence.DatabaseFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.util.*
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest {
    private lateinit var postgres: PostgreSQLContainer<Nothing>
    private lateinit var repository: UserRepositoryImpl

    @BeforeAll
    fun setup() {
        postgres = PostgreSQLContainer("postgres:15-alpine")
        postgres.start()
        
        val dataSource = DatabaseFactory.init(
            url = postgres.jdbcUrl,
            user = postgres.username,
            password = postgres.password
        )
        repository = UserRepositoryImpl(dataSource)
    }

    @Test
    fun `should save and retrieve admin by id`() {
        val admin = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        val saved = repository.save(admin)
        val found = repository.findById(admin.id)
        
        assertNotNull(found)
        assertEquals(saved.id, found?.id)
        assertEquals(saved.email, found?.email)
    }

    @Test
    fun `should save and retrieve admin by username`() {
        val admin = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser2",
            email = "test2@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        val saved = repository.save(admin)
        val found = repository.findByUsername(admin.username)
        
        assertNotNull(found)
        assertEquals(saved.id, found?.id)
        assertEquals(saved.username, found?.username)
    }

    @Test
    fun `should save and retrieve admin by email`() {
        val admin = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser3",
            email = "test3@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        val saved = repository.save(admin)
        val found = repository.findByEmail(admin.email)
        
        assertNotNull(found)
        assertEquals(saved.id, found?.id)
        assertEquals(saved.email, found?.email)
    }

    @Test
    fun `should find all admins`() {
        val admin1 = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser4",
            email = "test4@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        val admin2 = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser5",
            email = "test5@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        repository.save(admin1)
        repository.save(admin2)

        val allAdmins = repository.findAll()
        assertTrue(allAdmins.size >= 2)
        assertTrue(allAdmins.any { it.id == admin1.id })
        assertTrue(allAdmins.any { it.id == admin2.id })
    }

    @Test
    fun `should delete admin`() {
        val admin = AdminUser(
            id = UUID.randomUUID(),
            username = "testuser6",
            email = "test6@example.com",
            hashedPassword = "hashedpassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )

        repository.save(admin)
        repository.delete(admin.id)
        
        val found = repository.findById(admin.id)
        assertNull(found)
    }

    @AfterAll
    fun tearDown() {
        postgres.stop()
    }
}
