package dev.rubentxu.hodei.packages.app.features.packages.routes

import dev.rubentxu.hodei.packages.application.artifactmanagement.service.ArtifactPublicationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Placeholder for npm login request body
data class NpmLoginRequest(val name: String?, val password: String?) // Adjust based on actual npm login payload
data class NpmLoginResponse(val token: String, val ok: String, val username: String, val email: String)

fun Route.npmRoutes(artifactPublicationService: ArtifactPublicationService) { // Add AuthService if needed for login

    // PUT /npm/{repoId} - Publish package
    authenticate("bearerAuth") { // Assuming "bearerAuth" is the name of your Bearer auth provider
        put("/npm/{repoId}") {
            val repoId = call.parameters["repoId"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Repository ID is missing.")
            // TODO: Handle npm package publication (JSON body)
            // val packageData = call.receive<YourNpmPackageModel>()
            // artifactPublicationService.publishNpmPackage(repoId, packageData, call.principal<UserId>()!!)
            call.respond(HttpStatusCode.NotImplemented, "npm package publish for repo $repoId not implemented yet.")
        }
    }

    // GET /npm/{repoId}/{package} - Get package
    get("/npm/{repoId}/{package}") {
        val repoId = call.parameters["repoId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Repository ID is missing.")
        val packageName = call.parameters["package"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Package name is missing.")
        // TODO: Implement logic to get npm package details (JSON response)
        call.respond(HttpStatusCode.NotImplemented, "npm package get for $packageName in repo $repoId not implemented yet.")
    }

    // POST /-/npm/v1/login - npm login
    route("/-/npm/v1/login") {
        post {
            // val loginRequest = call.receive<NpmLoginRequest>() // Define NpmLoginRequest based on actual payload
            // TODO: Authenticate user (e.g., using AuthService) and generate token
            // val user = authService.login(loginRequest.name, loginRequest.password)
            // if (user != null) {
            //    val token = authService.generateNpmToken(user)
            //    call.respond(HttpStatusCode.OK, NpmLoginResponse(token, "Logged in", user.username, user.email))
            // } else {
            //    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            // }
            call.respond(
                HttpStatusCode.OK, // Placeholder success
                NpmLoginResponse("npm_0123456789abcdef", "Logged in", "myuser", "user@example.com")
            )
        }
    }
}
