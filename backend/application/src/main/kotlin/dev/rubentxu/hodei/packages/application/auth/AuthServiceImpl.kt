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
        // Input Validation
        if (command.username.isBlank()) {
            return Result.failure(AuthServiceError.ValidationFailed("Username cannot be blank"))
        }
        // Basic email validation: not blank and contains @
        if (command.email.isBlank() || !command.email.contains("@")) {
            return Result.failure(AuthServiceError.ValidationFailed("Invalid email format"))
        }
        // Basic password validation: at least 8 characters
        if (command.password.length < 8) {
            return Result.failure(AuthServiceError.ValidationFailed("Password must be at least 8 characters"))
        }

        return try {
            withContext(Dispatchers.IO) {
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
                    val savedUser = userRepository.save(adminUser)
                    // AuthenticationResult now includes more details from the saved user
                    Result.success(AuthenticationResult(savedUser.username, savedUser.email, "mock-token", "")) // Assuming token generation happens elsewhere or is static for now
                }
            }
        } catch (e: Exception) {
            // Consider logging the exception 'e' here for diagnostics
            // e.g., logger.error("Error during first admin registration", e)
            Result.failure(AuthServiceError.UnexpectedError("An unexpected error occurred: ${e.message}", e))
        }
    }

    override suspend fun login(command: LoginCommand): Result<AuthenticationResult, AuthServiceError> {
        return withContext(Dispatchers.IO) {
            // TODO: Implement login logic
            Result.success(AuthenticationResult("placeholder_user", "placeholder_email@example.com", "mock-token", ""))
        }
    }
}
