package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.auth.AuthServiceError.AdminAlreadyExists
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthServiceImplTest {
    private val userRepository = mockk<UserRepository>()
    private val authService = AuthServiceImpl(userRepository)

    @Test
    fun `testRegisterFirstAdmin success when no admin exists and conditions met`() =
        runTest {
            coEvery { userRepository.existsAdmin() } returns false

            val result =
                authService.registerFirstAdmin(
                    RegisterAdminCommand(
                        username = "testuser",
                        email = "test@example.com",
                        password = "password123",
                    ),
                )

            assertTrue(result.isSuccess)
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
        }
}
