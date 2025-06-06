package dev.rubentxu.hodei.packages.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import java.util.Date

class JwtTokenServiceTest : StringSpec() {

    private val secret = "test-secret-key-with-sufficient-length-for-hs256-algorithm-super-secure"
    private val issuer = "hodei-packages-test-issuer"
    private val audience = "hodei-packages-test-audience"
    private val expirationTimeMillis = 3600000L // 1 hour

    private val jwtTokenService = JwtTokenService(secret, issuer, audience, expirationTimeMillis)

    init {


        "generateToken should return a non-empty JWT string with correct claims" { 
            val userId = "user123"
            val username = "testuser"
            val email = "test@example.com"

            val token = jwtTokenService.generateToken(userId, username, email)

            token.shouldNotBeEmpty()

            val decodedJWT = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
                .verify(token)

            decodedJWT.subject shouldBe userId
            decodedJWT.getClaim("username").asString() shouldBe username
            decodedJWT.getClaim("email").asString() shouldBe email
            decodedJWT.issuer shouldBe issuer
            decodedJWT.audience shouldContain audience
            decodedJWT.expiresAt.time shouldBeGreaterThan Date().time
        }
            }
}
