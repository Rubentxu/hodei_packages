package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.shared.Result
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthServiceImpl(
    private val userRepository: UserRepository,
) : AuthService {
    override suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult, AuthServiceError> {
        return withContext(Dispatchers.IO) {
            if (userRepository.existsAdmin()) {
                Result.failure(AuthServiceError.AdminAlreadyExists)
            } else {
                // TODO: Hash password and create admin
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
