package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.domain.identityaccess.model.AdminUser
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.PasswordHasher
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.TokenService
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * Implementation of the AuthService interface that provides authentication and 
 * admin registration capabilities.
 *
 * This service handles:
 * - First admin user registration with validation
 * - User authentication via username/email and password
 * - Token generation for authenticated sessions
 */
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
) : AuthService {

    /**
     * Registers the first admin user in the system if no active admin exists.
     *
     * @param command Registration data containing username, email, and password
     * @return Result containing AuthenticationResult with user details and token on success, 
     *         or appropriate error on failure
     */
    override suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult> {
        // Validate user input
        if (command.username.isBlank()) {
            return Result.failure(AuthServiceError.ValidationFailed("Username cannot be blank"))
        }
        if (command.email.isBlank() || !command.email.contains("@")) {
            return Result.failure(AuthServiceError.ValidationFailed("Invalid email format"))
        }
        if (command.password.length < 8) {
            return Result.failure(AuthServiceError.ValidationFailed("Password must be at least 8 characters"))
        }

        return withContext(Dispatchers.IO) {
            try {
                // Check if admin already exists
                if (userRepository.existsAdmin()) {
                    return@withContext Result.failure(AuthServiceError.AdminAlreadyExists)
                }
                
                // Create and save new admin user
                val hashedPassword = passwordHasher.hash(command.password)
                val adminUser = AdminUser(
                    id = UUID.randomUUID(),
                    username = command.username,
                    email = command.email,
                    hashedPassword = hashedPassword,
                    isActive = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    lastAccess = null,
                )
                
                val savedUser = userRepository.save(adminUser)
                
                // Generate authentication token
                val token = tokenService.generateToken(
                    savedUser.id.toString(), 
                    savedUser.username, 
                    savedUser.email
                )
                
                Result.success(AuthenticationResult(savedUser.username, savedUser.email, token, ""))
            } catch (e: Exception) {
                Result.failure(AuthServiceError.UnexpectedError("Failed to register admin user: ${e.message}", e))
            }
        }
    }

    /**
     * Authenticates a user using username/email and password.
     *
     * @param command Login credentials containing username/email and password
     * @return Result containing AuthenticationResult with user details and token on success,
     *         or appropriate error on failure
     */
    override suspend fun login(command: LoginCommand): Result<AuthenticationResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Find user by email or username
                val user = userRepository.findByEmail(command.usernameOrEmail)
                    ?: userRepository.findByUsername(command.usernameOrEmail)
                    ?: return@withContext Result.failure(AuthServiceError.UserNotFound)

                // Verify password
                if (!passwordHasher.verify(command.password, user.hashedPassword)) {
                    return@withContext Result.failure(AuthServiceError.InvalidCredentials)
                }

                // Generate authentication token
                val token = tokenService.generateToken(
                    user.id.toString(), 
                    user.username, 
                    user.email
                )
                
                Result.success(AuthenticationResult(user.username, user.email, token, ""))
            } catch (e: Exception) {
                Result.failure(AuthServiceError.UnexpectedError("Failed during login process: ${e.message}", e))
            }
        }
    }
}
