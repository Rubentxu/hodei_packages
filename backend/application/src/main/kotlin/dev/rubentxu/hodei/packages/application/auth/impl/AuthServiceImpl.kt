package dev.rubentxu.hodei.packages.application.auth.impl

import dev.rubentxu.hodei.packages.application.auth.AuthService
import dev.rubentxu.hodei.packages.application.auth.AuthServiceError
import dev.rubentxu.hodei.packages.application.auth.AuthenticationResult
import dev.rubentxu.hodei.packages.application.auth.LoginCommand
import dev.rubentxu.hodei.packages.application.auth.RegisterAdminCommand
import dev.rubentxu.hodei.packages.application.shared.Result

// Temporary implementation for testing
class AuthServiceImpl : AuthService {
    override suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult, AuthServiceError> {
        return Result.failure(AuthServiceError.AdminAlreadyExists)
    }

    override suspend fun login(command: LoginCommand): Result<AuthenticationResult, AuthServiceError> {
        return Result.success(AuthenticationResult("placeholder_user", "placeholder_email@example.com", "token", "Login successful"))
    }
}
