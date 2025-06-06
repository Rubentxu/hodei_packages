package dev.rubentxu.hodei.packages.application.auth



interface AuthService {
    suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult>

    suspend fun login(command: LoginCommand): Result<AuthenticationResult>
}
