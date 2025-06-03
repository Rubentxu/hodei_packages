package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.auth.AuthServiceError.AdminAlreadyExists
import dev.rubentxu.hodei.packages.application.security.PasswordHasher
import dev.rubentxu.hodei.packages.application.security.TokenService
import dev.rubentxu.hodei.packages.domain.model.AdminUser
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
import io.mockk.coEvery
import java.time.Instant
import java.util.UUID
import io.mockk.coVerify
import io.mockk.mockk
import dev.rubentxu.hodei.packages.application.shared.Result // For mocking createAdmin return type
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import io.mockk.slot
import io.mockk.verify

class AuthServiceImplTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher: PasswordHasher = mockk()
    private val tokenService: TokenService = mockk()
    private lateinit var authService: AuthServiceImpl

    init {
        authService = AuthServiceImpl(userRepository, passwordHasher, tokenService)
    }

    @Test
    fun `registerFirstAdmin should return success when admin does not exist and data is valid`() = runTest {
        val command = RegisterAdminCommand("adminUser", "admin@example.com", "Password123")
        val hashedPassword = "hashedPassword"
        
        val mockSavedUserId = UUID.randomUUID()
        val mockSavedUser = AdminUser(
            id = mockSavedUserId,
            username = command.username,
            email = command.email,
            hashedPassword = hashedPassword,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastAccess = null
        )
        val expectedToken = "test-jwt-token"

        coEvery { userRepository.existsAdmin() } returns false
        coEvery { passwordHasher.hash(command.password) } returns hashedPassword
        coEvery { userRepository.save(any()) } returns mockSavedUser // Mock save to return our predefined user
        
        // Mock tokenService to return expectedToken when called with mockSavedUser's details
        every {
            tokenService.generateToken(
                userId = mockSavedUser.id.toString(), 
                username = mockSavedUser.username, 
                email = mockSavedUser.email
            )
        } returns expectedToken

        // Act
        val result = authService.registerFirstAdmin(command)

        // Assert
        assertTrue(result.isSuccess)
        val authResult = result.getOrNull()!!
        assertEquals(command.username, authResult.username)
        assertEquals(command.email, authResult.email)
        assertEquals(expectedToken, authResult.token)

        coVerify(exactly = 1) { userRepository.existsAdmin() }
        val capturedAdminUser = slot<AdminUser>()
        coVerify(exactly = 1) { userRepository.save(capture(capturedAdminUser)) }
        verify(exactly = 1) { passwordHasher.hash(command.password) }
        verify(exactly = 1) {
            tokenService.generateToken(
                userId = mockSavedUser.id.toString(), 
                username = mockSavedUser.username, 
                email = mockSavedUser.email
            )
        }

        // Optionally, verify details of the user passed to save, if different from mockSavedUser
        assertEquals(command.username, capturedAdminUser.captured.username)
        assertEquals(command.email, capturedAdminUser.captured.email)
        assertEquals(hashedPassword, capturedAdminUser.captured.hashedPassword)
        assertTrue(capturedAdminUser.captured.isActive)
    }

    @Test
    fun `testRegisterFirstAdmin failure when admin already exists`() =
        runTest {
            coEvery { userRepository.existsAdmin() } returns true

            val result =
                authService.registerFirstAdmin(
                    RegisterAdminCommand(
                        username = "testuser",
                        email = "test@example.com",
                        password = "password123",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(AdminAlreadyExists, result.errorOrNull())

            coVerify(exactly = 1) { userRepository.existsAdmin() }
            coVerify(exactly = 0) { passwordHasher.hash(any()) }
            coVerify(exactly = 0) { userRepository.save(any()) }
        }

    @Test
    fun `registerFirstAdmin failure when username is invalid`() = runTest {
        // TODO: Implement test: Given invalid username in command
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.ValidationFailed (or specific validation error)
        // And no repository or hasher methods should be called
        val command = RegisterAdminCommand(
            username = "", // Invalid username
            email = "test@example.com",
            password = "password123"
        )
        // coEvery { userRepository.existsAdmin() } returns false // Should not be called if validation fails early

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure for invalid username")
        assertEquals(AuthServiceError.ValidationFailed("Username cannot be blank"), result.errorOrNull())

        coVerify(exactly = 0) { userRepository.existsAdmin() }
        coVerify(exactly = 0) { passwordHasher.hash(any()) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerFirstAdmin failure when email is invalid`() = runTest {
        // TODO: Implement test: Given invalid email in command
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.ValidationFailed
        // And no repository or hasher methods should be called
        val command = RegisterAdminCommand(
            username = "testuser",
            email = "invalid-email", // Invalid email
            password = "password123"
        )

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure for invalid email")
        assertEquals(AuthServiceError.ValidationFailed("Invalid email format"), result.errorOrNull())

        coVerify(exactly = 0) { userRepository.existsAdmin() }
        coVerify(exactly = 0) { passwordHasher.hash(any()) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerFirstAdmin failure when password is invalid`() = runTest {
        // TODO: Implement test: Given invalid password in command
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.ValidationFailed
        // And no repository or hasher methods should be called
        val command = RegisterAdminCommand(
            username = "testuser",
            email = "test@example.com",
            password = "short" // Invalid password
        )

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure for invalid password")
        assertEquals(AuthServiceError.ValidationFailed("Password must be at least 8 characters"), result.errorOrNull())

        coVerify(exactly = 0) { userRepository.existsAdmin() }
        coVerify(exactly = 0) { passwordHasher.hash(any()) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerFirstAdmin failure when userRepository existsAdmin throws exception`() = runTest {
        // TODO: Implement test: Given userRepository.existsAdmin() throws exception
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.UnexpectedError
        val command = RegisterAdminCommand("testuser", "test@example.com", "password123")
        val expectedException = RuntimeException("DB error")
        coEvery { userRepository.existsAdmin() } throws expectedException

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure when existsAdmin throws exception")
        val actualError = result.errorOrNull()
        assertNotNull(actualError, "Error should not be null")
        assertTrue(actualError is AuthServiceError.UnexpectedError, "Error type should be UnexpectedError. Was: ${actualError!!::class.simpleName}")
        val unexpectedError = actualError as AuthServiceError.UnexpectedError
        assertEquals("An unexpected error occurred: ${expectedException.message ?: "Unknown error"}", unexpectedError.message)
        assertNotNull(unexpectedError.cause, "Cause should not be null")
        assertTrue(expectedException::class.isInstance(unexpectedError.cause), "Cause type should be ${expectedException::class.simpleName}. Was: ${unexpectedError.cause!!::class.simpleName}")
        assertEquals(expectedException.message, unexpectedError.cause?.message)

        coVerify(exactly = 1) { userRepository.existsAdmin() }
        coVerify(exactly = 0) { passwordHasher.hash(any()) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerFirstAdmin failure when passwordHasher throws exception`() = runTest {
        // TODO: Implement test: Given passwordHasher.hash() throws exception
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.UnexpectedError
        val command = RegisterAdminCommand("testuser", "test@example.com", "password123")
        val expectedException = RuntimeException("Hashing error")
        coEvery { userRepository.existsAdmin() } returns false
        coEvery { passwordHasher.hash(command.password) } throws expectedException

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure when passwordHasher throws exception")
        val actualError = result.errorOrNull()
        assertNotNull(actualError, "Error should not be null")
        assertTrue(actualError is AuthServiceError.UnexpectedError, "Error type should be UnexpectedError. Was: ${actualError!!::class.simpleName}")
        val unexpectedError = actualError as AuthServiceError.UnexpectedError
        assertEquals("An unexpected error occurred: ${expectedException.message ?: "Unknown error"}", unexpectedError.message)
        assertNotNull(unexpectedError.cause, "Cause should not be null")
        assertTrue(expectedException::class.isInstance(unexpectedError.cause), "Cause type should be ${expectedException::class.simpleName}. Was: ${unexpectedError.cause!!::class.simpleName}")
        assertEquals(expectedException.message, unexpectedError.cause?.message)

        coVerify(exactly = 1) { userRepository.existsAdmin() }
        coVerify(exactly = 1) { passwordHasher.hash(command.password) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `registerFirstAdmin failure when userRepository save throws exception`() = runTest {
        // TODO: Implement test: Given userRepository.save() throws exception
        // When registerFirstAdmin is called
        // Then should return Result.Failure with AuthServiceError.UnexpectedError
        val command = RegisterAdminCommand("testuser", "test@example.com", "password123")
        val hashedPassword = "hashedPassword123"
        val expectedException = RuntimeException("DB save error")

        coEvery { userRepository.existsAdmin() } returns false
        coEvery { passwordHasher.hash(command.password) } returns hashedPassword
        coEvery { userRepository.save(any()) } throws expectedException

        val result = authService.registerFirstAdmin(command)

        assertTrue(result.isFailure, "Expected failure when userRepository save throws exception")
        val actualError = result.errorOrNull()
        assertNotNull(actualError, "Error should not be null")
        assertTrue(actualError is AuthServiceError.UnexpectedError, "Error type should be UnexpectedError. Was: ${actualError!!::class.simpleName}")
        val unexpectedError = actualError as AuthServiceError.UnexpectedError
        assertEquals("An unexpected error occurred: ${expectedException.message ?: "Unknown error"}", unexpectedError.message)
        assertNotNull(unexpectedError.cause, "Cause should not be null")
        assertTrue(expectedException::class.isInstance(unexpectedError.cause), "Cause type should be ${expectedException::class.simpleName}. Was: ${unexpectedError.cause!!::class.simpleName}")
        assertEquals(expectedException.message, unexpectedError.cause?.message)

        coVerify(exactly = 1) { userRepository.existsAdmin() }
        coVerify(exactly = 1) { passwordHasher.hash(command.password) }
        coVerify(exactly = 1) { userRepository.save(any()) }
    }
}
