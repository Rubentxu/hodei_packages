package dev.rubentxu.hodei.packages.application.events.handlers

import dev.rubentxu.hodei.packages.application.identityaccess.service.AuthEventHandler
import dev.rubentxu.hodei.packages.domain.identityaccess.events.AuthEvent
import dev.rubentxu.hodei.packages.domain.identityaccess.model.AdminUser
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@ExperimentalCoroutinesApi
class AuthEventHandlerTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val handler = AuthEventHandler(userRepository)

    @Test
    fun `test handle AdminRegistered event`() =
        runTest {
            coEvery { userRepository.save(any<AdminUser>()) } returns mockk()

            val event: AuthEvent =
                AuthEvent.AdminRegistered(
                    adminId = UUID.randomUUID(),
                    username = "testuser",
                    email = "test@example.com",
                    timestamp = Instant.now(),
                )

            handler.handle(event)

            coVerify { userRepository.save(any<AdminUser>()) }
        }
}
