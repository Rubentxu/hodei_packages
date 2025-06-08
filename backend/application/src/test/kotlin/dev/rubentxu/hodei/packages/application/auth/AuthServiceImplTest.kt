package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.identityaccess.dto.LoginCommand
import dev.rubentxu.hodei.packages.application.identityaccess.dto.RegisterAdminCommand
import dev.rubentxu.hodei.packages.application.identityaccess.service.AuthServiceError
import dev.rubentxu.hodei.packages.application.identityaccess.service.AuthServiceError.AdminAlreadyExists
import dev.rubentxu.hodei.packages.application.identityaccess.service.AuthServiceImpl
import dev.rubentxu.hodei.packages.domain.ports.security.PasswordHasher
import dev.rubentxu.hodei.packages.domain.ports.security.TokenService
import dev.rubentxu.hodei.packages.domain.identityaccess.model.User
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AuthServiceImplTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher: PasswordHasher = mockk()
    private val tokenService: TokenService = mockk()
    private lateinit var authService: AuthServiceImpl

    init {
        authService = AuthServiceImpl(userRepository, passwordHasher, tokenService)
    }

    @Test
    fun `registerFirstAdmin should return success when admin does not exist and data is valid`() =
        runTest {
            val command = RegisterAdminCommand("adminUser", "admin@example.com", "Password123")
            val hashedPassword = "hashedPassword"

            val mockSavedUserId = UUID.randomUUID()
            val mockSavedUser =
                User(
                    id = mockSavedUserId,
                    username = command.username,
                    email = command.email,
                    hashedPassword = hashedPassword,
                    isActive = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    lastAccess = null,
                )
            val expectedToken = "test-jwt-token"

            every { userRepository.findAll() } returns emptyList()
            every { passwordHasher.hash(command.password) } returns hashedPassword
            every { userRepository.save(any()) } returns mockSavedUser

            every {
                tokenService.generateToken(
                    userId = mockSavedUser.id.toString(),
                    username = mockSavedUser.username,
                    email = mockSavedUser.email,
                )
            } returns expectedToken

            val result = authService.registerFirstAdmin(command)

            assertTrue(result.isSuccess)
            val authResult = result.getOrNull()!!
            assertEquals(mockSavedUser.username, authResult.username)
            assertEquals(mockSavedUser.email, authResult.email)
            assertEquals(expectedToken, authResult.token)

            verify(exactly = 1) { userRepository.findAll() }
            verify(exactly = 1) { passwordHasher.hash(command.password) }
            verify(exactly = 1) { userRepository.save(any()) }
            verify(exactly = 1) {
                tokenService.generateToken(
                    userId = mockSavedUser.id.toString(),
                    username = mockSavedUser.username,
                    email = mockSavedUser.email,
                )
            }
        }

    @Test
    fun `registerFirstAdmin should return failure when admin already exists`() = runTest {
        val command = RegisterAdminCommand("adminUser", "admin@example.com", "Password123")
        val existingAdmin = User(
            id = UUID.randomUUID(),
            username = "existingAdmin",
            email = "existing@example.com",
            hashedPassword = "hashedPassword",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null,
        )

        every { userRepository.findAll() } returns listOf(existingAdmin)

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertNotNull(error)
        assertTrue(error is AdminAlreadyExists)

        verify(exactly = 1) { userRepository.findAll() }
        verify(exactly = 0) { passwordHasher.hash(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { tokenService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login success with correct credentials`() = runTest {
        val command = LoginCommand("test@example.com", "password123")
        val email = command.usernameOrEmail
        val hashedPassword = "hashedPassword"
        val mockUser = User(
            id = UUID.randomUUID(),
            username = "testuser",
            email = command.usernameOrEmail,
            hashedPassword = hashedPassword,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null,
        )
        val expectedToken = "generated-jwt-token"

        every { userRepository.findByEmail(email) } returns mockUser
        every { passwordHasher.verify(command.password, hashedPassword) } returns true
        every { tokenService.generateToken(mockUser.id.toString(), mockUser.username, mockUser.email) } returns expectedToken

        val result = authService.login(command)

        assertTrue(result.isSuccess)
        val authResult = result.getOrNull()!!
        assertEquals(mockUser.username, authResult.username)
        assertEquals(mockUser.email, authResult.email)
        assertEquals(expectedToken, authResult.token)

        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 1) { passwordHasher.verify(command.password, hashedPassword) }
        verify(exactly = 1) { tokenService.generateToken(mockUser.id.toString(), mockUser.username, mockUser.email) }
    }

    @Test
    fun `login failure when user not found`() = runTest {
        val command = LoginCommand("nonexistent@example.com", "password123")

        every { userRepository.findByEmail(command.usernameOrEmail) } returns null

        val result = authService.login(command)

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertNotNull(error)
        assertTrue(error is AuthServiceError.InvalidCredentials)

        verify(exactly = 1) { userRepository.findByEmail(command.usernameOrEmail) }
        verify(exactly = 0) { passwordHasher.verify(any(), any()) }
        verify(exactly = 0) { tokenService.generateToken(any(), any(), any()) }
    }

    @Test
    fun `login failure when password is incorrect`() = runTest {
        val command = LoginCommand("test@example.com", "wrongpassword")
        val hashedPassword = "hashedPassword"
        val mockUser = User(
            id = UUID.randomUUID(),
            username = "testuser",
            email = command.usernameOrEmail,
            hashedPassword = hashedPassword,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null,
        )

        every { userRepository.findByEmail(command.usernameOrEmail) } returns mockUser
        every { passwordHasher.verify(command.password, hashedPassword) } returns false

        val result = authService.login(command)

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertNotNull(error)
        assertTrue(error is AuthServiceError.InvalidCredentials)

        verify(exactly = 1) { userRepository.findByEmail(command.usernameOrEmail) }
        verify(exactly = 1) { passwordHasher.verify(command.password, hashedPassword) }
        verify(exactly = 0) { tokenService.generateToken(any(), any(), any()) }
    }
} 