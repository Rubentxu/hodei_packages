package dev.rubentxu.hodei.packages.application.auth

import dev.rubentxu.hodei.packages.application.shared.Result

interface AuthService {
    suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult, AuthServiceError>

    suspend fun login(command: LoginCommand): Result<AuthenticationResult, AuthServiceError>
}
