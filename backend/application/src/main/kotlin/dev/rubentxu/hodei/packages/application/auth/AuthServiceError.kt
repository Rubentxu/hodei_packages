package dev.rubentxu.hodei.packages.application.auth

sealed class AuthServiceError {
    data class ValidationErrors(val errors: List<String>) : AuthServiceError()

    data object AdminAlreadyExists : AuthServiceError()

    data object UserNotFound : AuthServiceError()

    data object InvalidCredentials : AuthServiceError()

    data class UnknownError(val message: String) : AuthServiceError()
}
