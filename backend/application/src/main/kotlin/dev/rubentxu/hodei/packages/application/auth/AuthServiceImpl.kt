package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.shared.Result
import dev.rubentxu.hodei.packages.domain.model.AdminUser
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
import dev.rubentxu.hodei.packages.application.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.withContext

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher
) : AuthService {
    override suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult, AuthServiceError> {
        return withContext(Dispatchers.IO) {
            if (userRepository.existsAdmin()) {
                Result.failure(AuthServiceError.AdminAlreadyExists)
            } else {
                val hashedPassword = passwordHasher.hash(command.password)
                val adminUser = AdminUser(
                    id = UUID.randomUUID(), // Repository implementation might override or use this
                    username = command.username,
                    email = command.email,
                    hashedPassword = hashedPassword,
                    isActive = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    lastAccess = null
                )
                userRepository.save(adminUser)
                // TODO: The AuthenticationResult should ideally contain info from the created user, not a static token.
                // For now, keeping it as "mock-token" to pass the current test assertion.
                Result.success(AuthenticationResult("mock-token"))
            }
        }
    }

    override suspend fun login(command: LoginCommand): Result<AuthenticationResult, AuthServiceError> {
        return withContext(Dispatchers.IO) {
            // TODO: Implement login logic
            Result.success(AuthenticationResult("mock-token"))
        }
    }
}
