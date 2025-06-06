package dev.rubentxu.hodei.packages.application.identityaccess.service

import dev.rubentxu.hodei.packages.domain.identityaccess.events.AuthEvent
import dev.rubentxu.hodei.packages.domain.identityaccess.model.AdminUser
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthEventHandler(private val userRepository: UserRepository) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun handle(event: AuthEvent) {
        when (event) {
            is AuthEvent.AdminRegistered -> {
                val admin =
                    AdminUser(
                        id = event.adminId,
                        username = event.username,
                        email = event.email,
                        hashedPassword = "",
                        isActive = true,
                        createdAt = event.timestamp,
                        updatedAt = event.timestamp,
                        lastAccess = null,
                    )
                scope.launch {
                    userRepository.save(admin)
                }
            }
            else -> {
                // Manejar otros tipos de eventos o ignorarlos
            }
        }
    }
}