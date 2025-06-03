package dev.rubentxu.hodei.packages.infrastructure.repository

import com.zaxxer.hikari.HikariDataSource
import dev.rubentxu.hodei.packages.domain.model.AdminUser
import dev.rubentxu.hodei.packages.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserRepositoryImpl(private val dataSource: HikariDataSource) : UserRepository {
    override suspend fun save(user: AdminUser): AdminUser = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false // Start transaction
            try {
                val sql = """
                    INSERT INTO users (id, username, email, hashed_password, is_active, created_at, updated_at, last_access)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, user.id)
                    stmt.setString(2, user.username)
                    stmt.setString(3, user.email)
                    stmt.setString(4, user.hashedPassword)
                    stmt.setBoolean(5, user.isActive)
                    stmt.setTimestamp(6, Timestamp.from(user.createdAt))
                    stmt.setTimestamp(7, Timestamp.from(user.updatedAt))
                    stmt.setTimestamp(8, user.lastAccess?.let { Timestamp.from(it) })
                    stmt.executeUpdate()
                }
                conn.commit() // Commit transaction
                user
            } catch (e: Exception) {
                conn.rollback() // Rollback on error
                throw e
            }
        }
    }

    override suspend fun findByEmail(email: String): AdminUser? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM users WHERE email = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, email)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        rs.toAdminUser()
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun existsAdmin(): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val sql = "SELECT 1 FROM users LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next() // If there's any row, an admin exists
                }
            }
        }
    }

    private fun ResultSet.toAdminUser(): AdminUser = AdminUser(
        id = this.getObject("id", UUID::class.java),
        username = this.getString("username"),
        email = this.getString("email"),
        hashedPassword = this.getString("hashed_password"),
        isActive = this.getBoolean("is_active"),
        createdAt = this.getTimestamp("created_at").toInstant(),
        updatedAt = this.getTimestamp("updated_at").toInstant(),
        lastAccess = this.getTimestamp("last_access")?.toInstant()
    )

    private fun Instant.toLocalDateTime(): LocalDateTime = 
        LocalDateTime.ofInstant(this, ZoneOffset.UTC)

    private fun LocalDateTime.toInstant(): Instant = 
        this.toInstant(ZoneOffset.UTC)
}