package dev.rubentxu.hodei.packages.domain.repository

import dev.rubentxu.hodei.packages.domain.model.AdminUser

interface UserRepository {
    suspend fun save(admin: AdminUser): AdminUser

    suspend fun findByEmail(email: String): AdminUser?

    suspend fun existsAdmin(): Boolean
}
