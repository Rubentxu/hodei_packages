package dev.rubentxu.hodei.packages.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.rubentxu.hodei.packages.application.security.TokenService
import java.util.Date

class JwtTokenService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expirationTimeMillis: Long
) : TokenService {

    override fun generateToken(userId: String, username: String, email: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withSubject(userId)
            .withClaim("username", username)
            .withClaim("email", email)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expirationTimeMillis))
            .sign(Algorithm.HMAC256(secret))
    }

    // TODO: Add methods for token validation if needed as part of this service
    // fun validateToken(token: String): DecodedJWT? { ... }
}
