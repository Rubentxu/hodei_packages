package dev.rubentxu.hodei.packages.infrastructure.repository

import com.zaxxer.hikari.HikariDataSource
import dev.rubentxu.hodei.packages.domain.identityaccess.model.User
import dev.rubentxu.hodei.packages.domain.identityaccess.ports.UserRepository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

class UserRepositoryImpl(private val dataSource: HikariDataSource) : UserRepository {
    override fun save(user: User): User {
        dataSource.connection.use { conn ->
            conn.autoCommit = false // Start transaction
            try {
                val sql = """
                    INSERT INTO users (id, username, email, hashed_password, is_active, created_at, updated_at, last_access)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        username = EXCLUDED.username,
                        email = EXCLUDED.email,
                        hashed_password = EXCLUDED.hashed_password,
                        is_active = EXCLUDED.is_active,
                        created_at = EXCLUDED.created_at,
                        updated_at = EXCLUDED.updated_at,
                        last_access = EXCLUDED.last_access
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
                return user
            } catch (e: Exception) {
                conn.rollback() // Rollback on error
                throw e
            }
        }
    }

    override fun findById(id: UUID): User? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM users WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toAdminUser() else null
                }
            }
        }
    }

    override fun findByUsername(username: String): User? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM users WHERE username = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, username)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toAdminUser() else null
                }
            }
        }
    }

    override fun findByEmail(email: String): User? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM users WHERE email = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, email)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toAdminUser() else null
                }
            }
        }
    }

    override fun findAll(): List<User> {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM users"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val users = mutableListOf<User>()
                    while (rs.next()) {
                        users.add(rs.toAdminUser())
                    }
                    return users
                }
            }
        }
    }

    override fun delete(id: UUID) {
        dataSource.connection.use { conn ->
            val sql = "DELETE FROM users WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeUpdate()
            }
        }
    }

    private fun ResultSet.toAdminUser(): User = User(
        id = this.getObject("id", UUID::class.java),
        username = this.getString("username"),
        email = this.getString("email"),
        hashedPassword = this.getString("hashed_password"),
        isActive = this.getBoolean("is_active"),
        createdAt = this.getTimestamp("created_at").toInstant(),
        updatedAt = this.getTimestamp("updated_at").toInstant(),
        lastAccess = this.getTimestamp("last_access")?.toInstant()
    )
}