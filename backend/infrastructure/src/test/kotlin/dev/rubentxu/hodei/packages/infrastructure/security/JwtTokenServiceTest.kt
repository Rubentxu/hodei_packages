package dev.rubentxu.hodei.packages.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.util.Date

class JwtTokenServiceTest : StringSpec({

    private val secret = "test-secret-key-with-sufficient-length-for-hs256-algorithm-super-secure"
    private val issuer = "hodei-packages-test-issuer"
    private val audience = "hodei-packages-test-audience"
    private val expirationTimeMillis = 3600000L // 1 hour

    // Helper to create an instance for tests that expect successful generation
    private fun createWorkingTokenService(): JwtTokenService {
        return object : JwtTokenService(secret, issuer, audience, expirationTimeMillis) {
            override fun generateToken(userId: String, username: String, email: String): String {
                val now = System.currentTimeMillis()
                val algorithm = Algorithm.HMAC256(secret)
                return JWT.create()
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .withSubject(userId)
                    .withClaim("username", username)
                    .withClaim("email", email)
                    .withIssuedAt(Date(now))
                    .withExpiresAt(Date(now + expirationTimeMillis))
                    .sign(algorithm)
            }
        }
    }

    "generateToken should throw NotImplementedError for the initial empty implementation" {
        val tokenService = JwtTokenService(secret, issuer, audience, expirationTimeMillis)
        val userId = "user-123"
        val username = "testuser"
        val email = "test@example.com"

        shouldThrow<NotImplementedError> {
            tokenService.generateToken(userId, username, email)
        }
    }

    // This test will be used once the implementation is in place.
    // For now, it's commented out or can be marked as pending.
    /*
    "generateToken should return a non-empty JWT string with correct claims" {
        // Use a temporary working implementation for this test to pass
        // Once JwtTokenService is implemented, replace this with:
        // val tokenService = JwtTokenService(secret, issuer, audience, expirationTimeMillis)
        val tokenService = createWorkingTokenService() // Using helper for now

        val userId = "user-test-123"
        val username = "testusername"
        val email = "testuser@example.com"

        val token = tokenService.generateToken(userId, username, email)

        token.shouldNotBeEmpty()

        // Decode and verify claims
        val algorithm = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()

        val decodedJWT = verifier.verify(token)

        decodedJWT.subject shouldBe userId
        decodedJWT.getClaim("username").asString() shouldBe username
        decodedJWT.getClaim("email").asString() shouldBe email
        decodedJWT.issuer shouldBe issuer
        decodedJWT.audience.first() shouldBe audience // JWT library returns audience as a list
        decodedJWT.expiresAt.time shouldBeGreaterThan System.currentTimeMillis()
        decodedJWT.issuedAt.time shouldBe (decodedJWT.expiresAt.time - expirationTimeMillis)
    }
    */

})
