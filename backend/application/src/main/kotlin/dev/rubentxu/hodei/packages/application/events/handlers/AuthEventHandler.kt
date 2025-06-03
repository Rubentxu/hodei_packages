package dev.rubentxu.hodei.packages.application.events.handlers

import dev.rubentxu.hodei.packages.domain.events.auth.AuthEvent
import dev.rubentxu.hodei.packages.domain.model.AdminUser
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
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
