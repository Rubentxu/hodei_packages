package dev.rubentxu.hodei.packages.application.identityaccess.service

import dev.rubentxu.hodei.packages.application.identityaccess.dto.AuthenticationResult
import dev.rubentxu.hodei.packages.application.identityaccess.dto.LoginCommand
import dev.rubentxu.hodei.packages.application.identityaccess.dto.RegisterAdminCommand

interface AuthService {
    suspend fun registerFirstAdmin(command: RegisterAdminCommand): Result<AuthenticationResult>

    suspend fun login(command: LoginCommand): Result<AuthenticationResult>
}