package dev.rubentxu.hodei.packages.infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

object DatabaseFactory {
    fun init(
        url: String = "jdbc:postgresql://localhost:5432/hodei",
        user: String = "postgres", 
        password: String = "password"
    ): HikariDataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = url
            this.username = user
            this.password = password
            maximumPoolSize = 10
        }
        
        val dataSource = HikariDataSource(config)
        createTables(dataSource)
        return dataSource
    }

    private fun createTables(dataSource: HikariDataSource) {
        val userTableSql = """
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                hashed_password TEXT NOT NULL,
                is_active BOOLEAN DEFAULT TRUE NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                last_access TIMESTAMP NULL
            );
        """.trimIndent()

        val authLogsSql = """
            CREATE TABLE IF NOT EXISTS auth_logs (
                id UUID PRIMARY KEY,
                user_id UUID REFERENCES users(id) NOT NULL,
                event_type VARCHAR(50) NOT NULL,
                event_data TEXT NULL,
                created_at TIMESTAMP NOT NULL
            );
        """.trimIndent()

        try {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(userTableSql)
                    stmt.execute(authLogsSql)
                }
            }
            println("Tables created successfully or already exist.")
        } catch (e: SQLException) {
            println("Error creating tables: ${e.message}")
            e.printStackTrace()
        }
    }
}