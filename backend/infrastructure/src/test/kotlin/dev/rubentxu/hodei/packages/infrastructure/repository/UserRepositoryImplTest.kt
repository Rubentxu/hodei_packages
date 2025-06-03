package dev.rubentxu.hodei.packages.infrastructure.repository

import dev.rubentxu.hodei.packages.domain.model.AdminUser
import dev.rubentxu.hodei.packages.infrastructure.persistence.DatabaseFactory
import kotlinx.coroutines.test.runTest
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
    fun `should save and retrieve admin by email`() = runTest {
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
        val found = repository.findByEmail(admin.email)
        
        assertEquals(saved.id, found?.id)
        assertEquals(saved.email, found?.email)
    }

    @AfterAll
    fun tearDown() {
        postgres.stop()
    }
}
